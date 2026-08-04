#!/usr/bin/env python3
"""RoleCard 的零运行时 CI 质量门禁。

本脚本故意只读取源码与资源；它不导入 Forge、不调用 Gradle，也不启动 Java。
"""
from __future__ import annotations

import json
import hashlib
import re
import sys
import zipfile
from pathlib import Path

try:  # GitHub Hosted Runner (3.11+) 用标准库；本仓库的 Python 3.10 也可作静态检查。
    import tomllib
except ModuleNotFoundError:  # pragma: no cover - 仅 Python 3.10
    import tomli as tomllib


ROOT = Path(__file__).resolve().parents[2]
ERRORS: list[str] = []


def fail(message: str) -> None:
    ERRORS.append(message)


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def properties(path: Path) -> dict[str, str]:
    result: dict[str, str] = {}
    for number, line in enumerate(read(path).splitlines(), 1):
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        if "=" not in line:
            fail(f"{path}:{number}: 不是 key=value 属性")
            continue
        key, value = line.split("=", 1)
        result[key.strip()] = value.strip()
    return result


def check_resources(mod_id: str) -> None:
    resources = ROOT / "src/main/resources"
    for path in resources.rglob("*"):
        if not path.is_file():
            continue
        relative = path.relative_to(resources).as_posix()
        # META-INF 是 Forge 规定的唯一大写资源目录。
        if not relative.startswith("META-INF/") and relative != "pack.mcmeta":
            if relative != relative.lower() or not re.fullmatch(r"[a-z0-9_./-]+", relative):
                fail(f"资源路径必须为小写且仅含资源定位符字符: {relative}")
        if path.suffix == ".json":
            try:
                json.loads(read(path))
            except json.JSONDecodeError as exc:
                fail(f"JSON 不可解析: {relative}: {exc}")

    namespace = resources / "assets" / mod_id
    if not namespace.is_dir():
        fail(f"缺少模组资源命名空间: assets/{mod_id}")


def check_metadata(props: dict[str, str]) -> None:
    required = ("minecraft_version", "forge_version", "mod_id", "mod_version", "mod_group_id")
    for key in required:
        if not props.get(key):
            fail(f"gradle.properties 缺少 {key}")
    mod_id = props.get("mod_id", "")
    if not re.fullmatch(r"[a-z][a-z0-9_]{1,63}", mod_id):
        fail(f"mod_id 不符合 Forge 规则: {mod_id!r}")
    if props.get("minecraft_version") != "1.20.1":
        fail("本项目 CI 仅支持 Minecraft 1.20.1")
    if props.get("forge_version") != "47.4.10":
        fail("本项目 CI 锁定 Forge 47.4.10；修改时请同步更新工作流")

    toml_path = ROOT / "src/main/resources/META-INF/mods.toml"
    toml_text = read(toml_path)
    if 'modId="${mod_id}"' not in toml_text:
        fail("mods.toml 的 modId 必须直接引用 ${mod_id}")
    if 'version="${mod_version}"' not in toml_text:
        fail("mods.toml 的版本必须直接引用 ${mod_version}")
    expanded = re.sub(r"\$\{([A-Za-z0-9_]+)\}", lambda m: props.get(m.group(1), m.group(0)), toml_text)
    if "${" in expanded:
        fail("mods.toml 存在无法由 gradle.properties 替换的占位符")
    try:
        resolved = tomllib.loads(expanded)
        mods = resolved.get("mods", [])
        if not isinstance(mods, list) or not any(
            item.get("modId") == mod_id and item.get("version") == props.get("mod_version") for item in mods
        ):
            fail("替换 gradle.properties 后，mods.toml 未声明匹配的 modId 与 version")
    except tomllib.TOMLDecodeError as exc:
        fail(f"替换属性后的 mods.toml 不可解析: {exc}")
    main = ROOT / "src/main/java/com/rolecard/RoleCardMod.java"
    if not main.exists() or not re.search(r'MOD_ID\s*=\s*"' + re.escape(mod_id) + r'"', read(main)):
        fail("主模组类的 MOD_ID 与 gradle.properties 的 mod_id 不一致")


def check_resource_references(mod_id: str) -> None:
    assets = ROOT / "src/main/resources/assets" / mod_id
    java_files = list((ROOT / "src/main/java").rglob("*.java"))
    # 仅检查可静态确定的 rolecard:path / new ResourceLocation("rolecard", "path")。
    patterns = [
        re.compile(r'new\s+ResourceLocation\(\s*"' + re.escape(mod_id) + r'"\s*,\s*"([a-z0-9_./-]+)"\s*\)'),
        re.compile(r'"' + re.escape(mod_id) + r':([a-z0-9_./-]+)"'),
    ]
    for path in java_files:
        text = read(path)
        for pattern in patterns:
            for match in pattern.finditer(text):
                location = match.group(1)
                # 无扩展名的资源位置可能属于注册表而非文件；仅对显式扩展名做存在性断言。
                if "." in Path(location).name and not (assets / location).is_file():
                    fail(f"资源引用不存在: {path.relative_to(ROOT)} -> {mod_id}:{location}")


def check_common_side_and_network() -> None:
    java_root = ROOT / "src/main/java"
    forbidden = re.compile(r"\b(?:net\.minecraft\.client|com\.mojang\.blaze3d|org\.lwjgl)\b")
    for path in java_root.rglob("*.java"):
        relative = path.relative_to(java_root).as_posix()
        if "/client/" in f"/{relative}":
            continue
        if forbidden.search(read(path)):
            fail(f"common 侧出现客户端类引用: {relative}")

    network = ROOT / "src/main/java/com/rolecard/network/RoleCardNetwork.java"
    text = read(network) if network.exists() else ""
    registrations = re.findall(
        r"messageBuilder\(\s*([A-Za-z0-9_]+)\.class\s*,\s*nextId\+\+\s*,\s*NetworkDirection\.(PLAY_TO_(?:CLIENT|SERVER))\s*\)",
        text,
    )
    if len(registrations) < 3:
        fail("网络包必须显式注册消息 ID 与 PLAY_TO_CLIENT/PLAY_TO_SERVER 方向")
    packet_names = [item[0] for item in registrations]
    if len(packet_names) != len(set(packet_names)):
        fail("发现重复网络包注册")
    directions = dict(registrations)
    expected = {"CardSyncPacket": "PLAY_TO_CLIENT", "EditIdentityPacket": "PLAY_TO_SERVER", "PublicNamePacket": "PLAY_TO_CLIENT"}
    for name, direction in expected.items():
        if directions.get(name) != direction:
            fail(f"网络包 {name} 方向错误或未注册")
    c2s = ROOT / "src/main/java/com/rolecard/network/EditIdentityPacket.java"
    c2s_text = read(c2s) if c2s.exists() else ""
    if not re.search(r"getSender\(\).*?if\s*\(player\s*!=\s*null\)", c2s_text, re.S):
        fail("客户端到服务端身份包必须只使用 context.getSender() 且拒绝空发送者")


def check_wrapper() -> None:
    wrapper = ROOT / "gradle/wrapper/gradle-wrapper.properties"
    jar = ROOT / "gradle/wrapper/gradle-wrapper.jar"
    props = properties(wrapper)
    url = props.get("distributionUrl", "")
    if not url.startswith("https\\://services.gradle.org/distributions/gradle-8.8-bin.zip"):
        fail("Gradle Wrapper 必须固定到 HTTPS 的 Gradle 8.8 二进制发行包")
    if props.get("networkTimeout") != "10000":
        fail("Gradle Wrapper 必须设置 10000ms 网络超时")
    if not jar.is_file() or jar.stat().st_size < 10_000:
        fail("gradle-wrapper.jar 缺失或异常小")
        return
    try:
        with zipfile.ZipFile(jar) as archive:
            if "org/gradle/wrapper/GradleWrapperMain.class" not in archive.namelist():
                fail("gradle-wrapper.jar 不包含 GradleWrapperMain")
    except zipfile.BadZipFile:
        fail("gradle-wrapper.jar 不是有效 ZIP/JAR")
    expected_sha256 = "ed2c26eba7cfb93cc2b7785d05e534f07b5b48b5e7fc941921cd098628abca58"
    actual_sha256 = hashlib.sha256(jar.read_bytes()).hexdigest()
    if actual_sha256 != expected_sha256:
        fail(f"gradle-wrapper.jar SHA-256 不匹配: {actual_sha256}")


def check_dedicated_server_contract() -> None:
    """防止以后无意放松固定 Forge 坐标或安装器完整性校验。"""
    path = ROOT / "scripts/ci/dedicated-server-smoke.sh"
    if not path.exists():
        fail("缺少专服真实 Smoke 脚本")
        return
    text = read(path)
    required = (
        'MC_VERSION="1.20.1"',
        'FORGE_VERSION="47.4.10"',
        'https://maven.minecraftforge.net/net/minecraftforge/forge/${FORGE_COORD}/forge-${FORGE_COORD}-installer.jar',
        '"${installer_url}.sha1"',
        'curl --fail --location --retry 1',
        'sha1sum "$installer"',
        '"$actual_sha1" == "$expected_sha1"',
        'test -x "$SERVER/run.sh"',
        'libraries/net/minecraftforge/forge/$FORGE_COORD',
    )
    for needle in required:
        if needle not in text:
            fail(f"专服脚本缺少固定版本/下载完整性门禁: {needle}")


def check_client_smoke_probe() -> None:
    """客户端自动退出必须由不参与发布的 CI 源集完成，不能以销毁 X 窗口伪造成功。"""
    probe = ROOT / "src/ci/java/com/rolecard/ci/ClientSmokeProbe.java"
    build = read(ROOT / "build.gradle")
    client_script = read(ROOT / "scripts/ci/client-smoke.sh")
    if not probe.is_file():
        fail("缺少仅 CI 使用的客户端主菜单探针")
        return
    probe_text = read(probe)
    required_probe = (
        "TitleScreen",
        "TickEvent.ClientTickEvent",
        "ROLECARD_CI_TITLE_SCREEN_READY",
        "Minecraft.getInstance().stop()",
        "src/ci",
    )
    for needle in required_probe:
        if needle not in probe_text and needle != "src/ci":
            fail(f"客户端探针缺少主菜单正常退出契约: {needle}")
    for needle in ("sourceSets {", "java.srcDir 'src/ci/java'", "source sourceSets.ci", "ciClasses"):
        if needle not in build:
            fail(f"build.gradle 缺少 CI-only 客户端探针源集配置: {needle}")
    if "ROLECARD_CI_TITLE_SCREEN_READY" not in client_script or "windowclose" in client_script:
        fail("客户端 smoke 必须等待主菜单探针且不得直接销毁 X 窗口")


def main() -> int:
    props = properties(ROOT / "gradle.properties")
    check_metadata(props)
    check_resources(props.get("mod_id", "rolecard"))
    check_resource_references(props.get("mod_id", "rolecard"))
    check_common_side_and_network()
    check_wrapper()
    check_dedicated_server_contract()
    check_client_smoke_probe()
    if ERRORS:
        print("质量门禁失败：", file=sys.stderr)
        for error in ERRORS:
            print(f"- {error}", file=sys.stderr)
        return 1
    print("质量门禁通过：资源、元数据、侧别、网络与 Wrapper 均符合当前 rolecard 项目约束。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
