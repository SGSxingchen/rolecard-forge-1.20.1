#!/usr/bin/env python3
"""从 Gradle 输出中挑出唯一可发布的 rolecard Jar，并生成 SHA-256 清单。"""
from __future__ import annotations

import hashlib
import re
import sys
import zipfile
from pathlib import Path

try:
    import tomllib
except ModuleNotFoundError:  # Python 3.10 本地静态检查兼容
    import tomli as tomllib


def fail(message: str) -> None:
    raise SystemExit(f"发布包校验失败：{message}")


root = Path(__file__).resolve().parents[2]
output = root / "dist"
output.mkdir(exist_ok=True)
props = {}
for line in (root / "gradle.properties").read_text(encoding="utf-8").splitlines():
    if "=" in line and not line.lstrip().startswith("#"):
        key, value = line.split("=", 1)
        props[key.strip()] = value.strip()
mod_id, version = props["mod_id"], props["mod_version"]
candidates = [
    path for path in (root / "build/libs").glob("*.jar")
    if not re.search(r"-(?:sources|javadoc|dev|plain)\.jar$", path.name)
]
if len(candidates) != 1:
    fail(f"期望唯一可发布 Jar，实际为 {[p.name for p in candidates]}")
jar = candidates[0]
try:
    with zipfile.ZipFile(jar) as archive:
        if "META-INF/mods.toml" not in archive.namelist():
            fail("Jar 中缺少 META-INF/mods.toml")
        if any(name.endswith("ClientSmokeProbe.class") for name in archive.namelist()):
            fail("CI 客户端探针被错误打入发布 Jar")
        mods = tomllib.loads(archive.read("META-INF/mods.toml").decode("utf-8"))
        mods_list = mods.get("mods", [])
        if not any(item.get("modId") == mod_id for item in mods_list):
            fail(f"Jar 中 mods.toml 未声明 modId={mod_id}")
except zipfile.BadZipFile:
    fail(f"{jar.name} 不是有效 Jar")

target = output / f"{mod_id}-{version}.jar"
target.write_bytes(jar.read_bytes())
digest = hashlib.sha256(target.read_bytes()).hexdigest()
(output / "SHA256SUMS").write_text(f"{digest}  {target.name}\n", encoding="utf-8")
print(f"jar={target.name}")
print(f"sha256={digest}")
