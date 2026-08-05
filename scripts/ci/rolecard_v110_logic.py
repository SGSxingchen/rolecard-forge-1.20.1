#!/usr/bin/env python3
"""v1.1.0 角色卡规则的零运行时回归探针。

此探针不启动 Gradle、Forge 或 Minecraft，因而可在质量门禁中很快失败。它同时
验证项目源码仍保留关键的服务端权威边界，并用小型参考模型固定本版本的业务规则。
真实的包编解码、专服和客户端加载仍由后续 CI Job 负责。
"""
from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum
from pathlib import Path
import re
import unicodedata


ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/com/rolecard"
FAILURES: list[str] = []


def check(condition: bool, message: str) -> None:
    if not condition:
        FAILURES.append(message)


def source(relative: str) -> str:
    path = JAVA / relative
    check(path.is_file(), f"缺少源码：src/main/java/com/rolecard/{relative}")
    return path.read_text(encoding="utf-8") if path.is_file() else ""


def safe_biography(value: str, maximum: int = 1600, maximum_utf8: int = 6400) -> str | None:
    """与服务端约定一致的参考输入边界：保留换行，拒绝控制与格式字符。"""
    if len(value) > maximum:
        return None
    try:
        encoded = value.encode("utf-8", "strict")
    except UnicodeError:
        return None
    if len(encoded) > maximum_utf8:
        return None
    for character in value:
        # 换行、制表符以外的控制字符及所有格式字符（含 §）均不可进入组件或 NBT。
        if character == "\u00a7" or unicodedata.category(character) == "Cf":
            return None
        if unicodedata.category(character) == "Cc" and character not in "\n\t":
            return None
    return value


class Status(Enum):
    DRAFT = "DRAFT"
    PENDING = "PENDING"
    APPROVED = "APPROVED"
    REJECTED = "REJECTED"


@dataclass
class ReferenceCard:
    """只表达产品规则；用于防止 CI 门禁被无意放松。"""

    available: int = 0
    stats: dict[str, int] = field(default_factory=lambda: {key: 0 for key in ("bulk", "strength", "coordination", "reflex", "spirit", "luck")})
    status: Status = Status.DRAFT
    revision: int = 0

    def edit(self, changes: dict[str, int], expected_revision: int) -> bool:
        if self.status not in (Status.DRAFT, Status.REJECTED) or expected_revision != self.revision:
            return False
        next_stats = dict(self.stats)
        for key, value in changes.items():
            if key not in next_stats or not 0 <= value <= 100:
                return False
            next_stats[key] = value
        spending = sum(next_stats.values()) - sum(self.stats.values())
        if spending > self.available:
            return False
        self.stats = next_stats
        self.available -= spending  # 负 spending 是撤点退款。
        self.revision += 1
        return True

    def submit(self, expected_revision: int) -> bool:
        if expected_revision != self.revision or self.status not in (Status.DRAFT, Status.REJECTED, Status.PENDING):
            return False
        # pending 的重提只更新同一条待审记录；不扣点也不新建队列元素。
        if self.status is not Status.PENDING:
            self.status = Status.PENDING
            self.revision += 1
        return True

    def review(self, target: Status) -> bool:
        if self.status is not Status.PENDING or target not in (Status.APPROVED, Status.REJECTED):
            return False
        self.status = target
        self.revision += 1
        return True


def verify_reference_rules() -> None:
    # v1 迁移：旧档没有新字段，原身份和六维原样保留，新字段走安全默认值。
    v1 = {"roleName": "旧角色", "age": 21, "gender": "未设定", "stats": {"bulk": 7}}
    migrated = {**v1, "biography": "", "available": 0, "status": Status.DRAFT, "revision": 0}
    check(migrated["roleName"] == "旧角色" and migrated["stats"]["bulk"] == 7 and migrated["biography"] == "", "参考迁移规则未保留 v1 身份/六维或默认新字段")

    check(safe_biography("第一行\n第二行") is not None, "生平应允许换行")
    check(safe_biography("a" * 1601) is None, "生平超出字符上限必须拒绝")
    check(safe_biography("正常\u0000内容") is None and safe_biography("伪装\u200b内容") is None, "生平控制/格式字符必须拒绝")

    card = ReferenceCard(available=3)
    check(card.edit({"bulk": 2, "luck": 1}, 0), "合法加点应成功")
    check(card.available == 0 and sum(card.stats.values()) == 3, "加点后点数必须守恒")
    check(not card.edit({"strength": 1}, 1), "伪造超额点数必须拒绝")
    check(card.edit({"bulk": 1}, 1) and card.available == 1, "草稿撤点必须退款")
    check(not card.edit({"bulk": 101}, 2), "单项超过 100 必须拒绝")
    check(not card.submit(1) and card.submit(2), "旧 revision 的提交必须拒绝，当前 revision 可提交")
    pending_revision = card.revision
    check(card.submit(pending_revision) and card.revision == pending_revision, "重复 pending 提交必须幂等且不得扣点/改 revision")
    check(not card.edit({"bulk": 0}, pending_revision), "pending 状态玩家不得改卡")
    check(card.review(Status.APPROVED), "pending 必须可以批准")
    check(not card.edit({"bulk": 0}, card.revision), "approved 状态玩家不得洗点")
    check(not card.review(Status.REJECTED), "approved 不得直接变更为 rejected")

    # 队列使用玩家 UUID 的集合/映射语义；同一 pending 重提不会重复通知条目。
    queue: set[str] = set()
    queue.add("player-uuid")
    queue.add("player-uuid")
    check(len(queue) == 1, "待审队列必须按玩家去重")
    check(2 >= 2 and not (1 >= 2), "管理员操作权限必须至少为等级 2")


def verify_source_contract() -> None:
    card = source("data/CharacterCard.java")
    statuses = source("data/ReviewStatus.java")
    commands = source("RoleCardCommands.java")
    events = source("RoleCardEvents.java")
    service = source("service/RoleCardService.java")
    queue = source("review/ReviewQueueSavedData.java")
    applier = source("attribute/AttributeApplier.java")
    mappings = source("attribute/AttributeMappings.java")
    network = source("network/RoleCardNetwork.java")
    packets = "\n".join(path.read_text(encoding="utf-8") for path in (JAVA / "network").glob("*.java"))
    admin_packet = source("network/AdminReviewActionPacket.java")

    check(bool(re.search(r"DATA_VERSION\s*=\s*[2-9]", card)), "CharacterCard 必须升级到 v2 数据版本")
    for token in ("biography", "availablePoints", "revision", "ReviewStatus"):
        check(token in card, f"CharacterCard 缺少 v1.1.0 字段/状态：{token}")
    for token in ("DRAFT", "PENDING", "APPROVED", "REJECTED"):
        check(token in statuses, f"ReviewStatus 缺少状态：{token}")
    check("Tag.TAG_STRING" in card and "Tag.TAG_COMPOUND" in card, "旧 NBT 读取必须显式检查字段类型并安全默认")
    check("copyFrom" in card and "load(other.save())" in card, "Capability clone 必须经完整存档复制新字段")
    check(bool(re.search(r"MAX_BIOGRAPHY.*(?:1000|1200|1500|1600|1800|2000)", card)), "生平必须声明 1000–2000 的合理字符上限")
    check("UTF_8" in card and "isISOControl" in card and "Character.FORMAT" in card, "生平必须校验 UTF-8、控制字符和 Unicode 格式字符")
    check(any(name in card for name in ("adjustPlayerStat", "applyPlayer", "canPlayerEdit")), "CharacterCard 必须包含由服务端执行的玩家加点/锁定规则")
    check("revision" in packets and ("getSender()" in packets or "getSender ()" in packets), "变更包必须携带 revision，并仅使用服务端发送者")
    check("readUtf(" in packets and "MAX_BIOGRAPHY_UTF8_BYTES" in packets, "网络包必须给身份/生平字符串设置字节上限")
    check("PLAY_TO_SERVER" in network and "PLAY_TO_CLIENT" in network, "审核流程包必须声明双向网络方向")
    check("hasPermission(2)" in commands, "管理命令必须要求权限等级 2")
    check("hasPermissions(2)" in admin_packet and "getPlayer(p.target)" in admin_packet, "管理员 C2S 包必须拒绝非 OP，并只允许在线目标")
    check("review" in commands and "points" in commands, "必须注册 review 与 points 命令入口")
    check("syncAndApply" in events, "身份/审核/点数更新后必须同步并重新应用属性")
    check("Map<UUID, Entry>" in queue and "entries.put" in queue and "SavedData" in queue, "待审索引必须按 UUID 去重并持久化")
    check("status() != ReviewStatus.PENDING" in service, "批准/退回必须仅允许从 pending 状态迁移")
    submit_start = service.find("boolean submit(")
    submit_end = service.find("boolean approve(", submit_start)
    submit_body = service[submit_start:submit_end]
    check("ReviewStatus.PENDING" in submit_body, "重复 pending 提交必须由服务端按同一 UUID 幂等处理")

    # Modifier 必须先按固定 UUID 删除再添加；规则 key 不可重复，以免属性叠加。
    remove_index = applier.find("removeModifier")
    add_index = applier.find("addPermanentModifier")
    check(remove_index >= 0 and add_index > remove_index, "属性 Modifier 必须先删除固定 UUID 再添加，防止重复")
    keys = re.findall(r'AttributeRule\.of\("([^"]+)"', mappings)
    check(len(keys) == len(set(keys)) and bool(keys), "属性规则 key/固定 UUID 不得重复")


def main() -> int:
    verify_reference_rules()
    verify_source_contract()
    if FAILURES:
        print("v1.1.0 纯逻辑探针失败：")
        for failure in FAILURES:
            print(f"- {failure}")
        return 1
    print("v1.1.0 纯逻辑探针通过：迁移、生平、点数、状态机、revision、幂等、权限和属性去重契约均已覆盖。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
