#!/usr/bin/env python3
"""生成不可被 skipped 掩盖的 CI 回归覆盖矩阵。"""
from __future__ import annotations

import os
import sys
from pathlib import Path


jobs = {
    "quality-gates": os.environ.get("QUALITY_GATES", "missing"),
    "build": os.environ.get("BUILD", "missing"),
    "dedicated-server": os.environ.get("DEDICATED_SERVER", "missing"),
    "client-smoke": os.environ.get("CLIENT_SMOKE", "missing"),
}
rows = [
    ("发布包 / mods.toml / modId", "完整自动覆盖", "build：真实 clean/build 后校验唯一 Jar 与元数据"),
    ("资源、JSON/TOML、modId、侧别、网络注册", "完整自动覆盖（静态）", "quality-gates：仅源码/资源解析，不启动 Java"),
    ("Forge 专服加载 rolecard、命令注册、save-all、stop", "完整自动覆盖", "dedicated-server：真实 Forge 47.4.10 专服"),
    ("单客户端初始化与稳定退出", "完整自动覆盖", "client-smoke：Xvfb + Mesa 的真实 Forge Client"),
    ("Capability 默认值、NBT 往返与容错", "部分覆盖", "静态结构门禁；尚无 CI 专用 GameTest"),
    ("六维 AttributeModifier 不重复累积", "部分覆盖", "静态检查 removeModifier 后 addPermanentModifier；未做带玩家生命周期实测"),
    ("管理员权限拒绝、恶意越权网络请求", "部分覆盖", "静态检查 requires(permission 2)、C2S 仅取 context sender；未做伪造客户端实测"),
    ("死亡 clone、重连、换维度同步", "部分覆盖", "静态检查对应事件钩子；未做玩家端到端实测"),
    ("聊天 / 头顶 / Tab 三模式", "人工待测", "需要真实玩家观察显示效果"),
    ("角色卡 UI 交互", "人工待测", "需要输入、保存及非法输入反馈检查"),
    ("双客户端显示一致性", "人工待测", "本轮未加入成本高且易波动的双客户端联机矩阵"),
]
out = Path("ci-artifacts/regression-report.md")
out.parent.mkdir(parents=True, exist_ok=True)
lines = ["# RoleCard CI 回归报告", "", "## 必需 Job 状态", "", "| Job | 状态 |", "| --- | --- |"]
lines.extend(f"| `{name}` | **{status}** |" for name, status in jobs.items())
lines += ["", "## 覆盖矩阵", "", "| 验证项 | 覆盖级别 | 证据 / 边界 |", "| --- | --- | --- |"]
lines.extend(f"| {a} | {b} | {c} |" for a, b, c in rows)
out.write_text("\n".join(lines) + "\n", encoding="utf-8")
bad = {name: status for name, status in jobs.items() if status != "success"}
if bad:
    print(f"必需 Job 未全绿（failed/skipped/missing 均不允许）：{bad}", file=sys.stderr)
    sys.exit(1)
print(out)
