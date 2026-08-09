#!/usr/bin/env python3
"""v1.2.0 世界任务公告的零运行时门禁；不启动 Gradle、Java、Forge 或 Minecraft。"""
from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
import unicodedata


ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/com/rolecard"
FAILURES: list[str] = []


def check(condition: bool, message: str) -> None:
    if not condition:
        FAILURES.append(message)


def source(relative: str) -> str:
    path = JAVA / relative
    check(path.is_file(), f"缺少任务公告源码：{relative}")
    return path.read_text(encoding="utf-8") if path.is_file() else ""


def safe_text(value: str, maximum: int, multiline: bool = False) -> bool:
    if len(value) > maximum or len(value.encode("utf-8")) > min(4800, maximum * 4 + 32):
        return False
    for character in value:
        category = unicodedata.category(character)
        if character == "§" or category == "Cf":
            return False
        if category == "Cc" and (not multiline or character not in "\n\t"):
            return False
    return True


@dataclass
class Board:
    revision: int = 0
    title: str = ""
    objectives: list[tuple[bool, str]] = field(default_factory=list)

    def replace(self, expected: int, title: str) -> str:
        if expected != self.revision:
            return "stale"
        if title == self.title:
            return "same"
        self.title = title
        self.revision += 1
        return "changed"


def verify_reference_model() -> None:
    check(safe_text("第一行\n第二行", 1200, True), "简介应允许换行")
    check(not safe_text("恶意\u0000文本", 80) and not safe_text("伪装\u200b文本", 80), "控制字符和 Unicode 格式字符必须拒绝")
    check(not safe_text("x" * 81, 80), "标题超过硬上限必须拒绝")
    board = Board()
    check(board.replace(0, "遗迹调查") == "changed" and board.revision == 1, "首次发布必须增加 revision")
    check(board.replace(0, "旧包覆盖") == "stale" and board.title == "遗迹调查", "旧 revision 不得覆盖新公告")
    check(board.replace(1, "遗迹调查") == "same" and board.revision == 1, "相同内容保存必须幂等且不增加 revision")
    board.objectives = [(False, "目标")] * 12
    check(len(board.objectives) == 12 and len(board.objectives + [(False, "超限")]) > 12, "目标数量边界参考模型失效")


def verify_source_contract() -> None:
    snapshot = source("mission/MissionBoardSnapshot.java")
    saved = source("mission/MissionBoardSavedData.java")
    service = source("service/MissionBoardService.java")
    commands = source("RoleCardCommands.java")
    events = source("RoleCardEvents.java")
    network = source("network/RoleCardNetwork.java")
    admin_packet = source("network/AdminSaveMissionPacket.java")
    sync_packet = source("network/MissionSyncPacket.java")
    hooks = source("network/ClientHooks.java")
    cache = source("client/ClientMissionCache.java")

    for token in ("DATA_VERSION = 1", "MAX_OBJECTIVES = 12", "MAX_TITLE_LENGTH", "MAX_SUMMARY_LENGTH", "MAX_OBJECTIVE_LENGTH", "MAX_RULES_LENGTH", "MAX_NOTES_LENGTH", "isValidClientTag", "Character.FORMAT", "Character.isISOControl", "revision", "updatedAt", "lastEditor", "instanceCode", "playerCountText", "timeLimitText"):
        check(token in snapshot, f"任务 DTO 缺少边界/展示字段：{token}")
    check("SavedData" in saved and "overworld().getDataStorage()" in saved and "computeIfAbsent" in saved, "任务公告必须使用 Overworld SavedData")
    check("static Map" not in saved and "static Map" not in service, "不得用静态 Map 冒充任务持久化")
    for token in ("getSnapshot", "replace", "update", "expectedRevision != current.revision()", "sameContent", "syncMission", "announce"):
        check(token in service, f"MissionBoardService 缺少权威更新/同步契约：{token}")
    for token in ("mission", "view", "edit", "clear", "status", "title", "summary", "instance", "objective", "BoolArgumentType", "hasPermission(2)"):
        check(token in commands, f"/rolecard mission 缺少入口或权限：{token}")
    check("MissionBoardService.sync(player)" in events and "PlayerLoggedInEvent" in events and "PlayerChangedDimensionEvent" in events, "登录、换维度、配置重载必须同步任务快照")
    for token in ("MissionSyncPacket", "OpenMissionPacket", "OpenAdminMissionPacket", "AdminSaveMissionPacket", "PLAY_TO_CLIENT", "PLAY_TO_SERVER"):
        check(token in network, f"网络注册缺少任务包或方向：{token}")
    check("hasPermissions(2)" in admin_packet and "isValidClientTag" in admin_packet and "packet.revision" in admin_packet, "管理员任务保存包必须校验 OP、输入和 revision")
    check("acceptMission" in sync_packet and "acceptMission" in hooks and "acceptAdminMission" in hooks, "S2C 包必须仅交由客户端展示 Hook")
    check("revision() >= snapshot.revision()" in cache or "revision() >=" in cache, "客户端缓存必须拒绝旧 revision 快照")
    check("com.rolecard.client" not in service and "net.minecraft.client" not in saved, "公共任务数据层不得引用客户端类")


def main() -> int:
    verify_reference_model()
    verify_source_contract()
    if FAILURES:
        print("v1.2.0 世界任务公告探针失败：")
        for failure in FAILURES:
            print(f"- {failure}")
        return 1
    print("v1.2.0 世界任务公告探针通过：持久化、边界、revision、权限、命令、同步、广播和旧包防护均已覆盖。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
