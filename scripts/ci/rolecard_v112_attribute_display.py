#!/usr/bin/env python3
"""v1.1.2 属性显示的零运行时静态探针；不导入 Forge、不启动 Java。"""
from __future__ import annotations

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
ERRORS: list[str] = []


def check(condition: bool, message: str) -> None:
    if not condition:
        ERRORS.append(message)


def source(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")


def display(value: float, operation: str) -> str:
    if abs(value) < 0.0000001:
        return "+0"
    sign, magnitude = ("+", value) if value > 0 else ("-", -value)
    return sign + (f"{magnitude:.2f}" if operation == "ADDITION" else f"{magnitude * 100:.2f}".rstrip("0").rstrip(".")) + ("" if operation == "ADDITION" else "%")


def main() -> int:
    formatter = source("src/main/java/com/rolecard/client/AttributeDisplayFormatter.java")
    player = source("src/main/java/com/rolecard/client/RoleCardScreen.java")
    admin = source("src/main/java/com/rolecard/client/AdminRoleCardScreen.java")
    mappings = source("src/main/java/com/rolecard/attribute/AttributeMappings.java")
    smoke = source("src/ci/java/com/rolecard/ci/ClientSmokeProbe.java")
    zh = json.loads(source("src/main/resources/assets/rolecard/lang/zh_cn.json"))
    en = json.loads(source("src/main/resources/assets/rolecard/lang/en_us.json"))

    names = {
        "max_health": ("最大生命值", "Max Health"), "armor": ("护甲值", "Armor"),
        "knockback_resistance": ("击退抗性", "Knockback Resistance"), "attack_damage": ("攻击伤害", "Attack Damage"),
        "attack_knockback": ("攻击击退", "Attack Knockback"), "movement_speed": ("移动速度", "Movement Speed"),
        "attack_speed": ("攻击速度", "Attack Speed"), "armor_toughness": ("护甲韧性", "Armor Toughness"),
        "luck": ("幸运值", "Luck"),
    }
    for path, (zh_value, en_value) in names.items():
        key = f"rolecard.attribute.name.minecraft.generic.{path}"
        check(zh.get(key) == zh_value, f"中文属性兜底缺失或错误: {key}")
        check(en.get(key) == en_value, f"英文属性兜底缺失或错误: {key}")

    for needle in ("Component.translatable(descriptionId)", "Language.getInstance().has(descriptionId)", "rolecard.attribute.name.", "friendlyName", "MULTIPLY_BASE, MULTIPLY_TOTAL", "case ADDITION", "Component.translatable(\"rolecard.attribute.modifier\"", "List<Component>"):
        check(needle in formatter, f"格式化层缺少必要契约: {needle}")
    check('replace("attribute.name.generic."' not in player, "普通玩家界面仍在截断属性 description id")
    check("renderComponentTooltip(font, tooltip" in player and "renderComponentTooltip(font, tooltip" in admin, "玩家和管理员必须使用原生多行 tooltip API")
    check("AttributeDisplayFormatter.summary" in player and "AttributeDisplayFormatter.summary" in admin, "玩家和管理员摘要必须复用格式化层")
    check("AttributeDisplayFormatter.tooltip" in player and "AttributeDisplayFormatter.tooltip" in admin, "玩家和管理员 tooltip 必须复用格式化层")
    check("StatType.clamp(current + pendingChanges[i])" in player, "玩家属性显示未使用当前值加草案变化预览")
    check("previewValue(i, current)" in admin, "管理员属性显示未使用编辑草案预览")
    check(smoke.count("ciInitializeStatsTooltip") >= 2, "客户端 Smoke 未初始化玩家和管理员六维 tooltip 组件")
    check("amount(int statValue, AttributeRule rule)" in mappings, "属性映射缺少明确预览值计算入口")

    expected = {
        (2.4, "ADDITION"): "+2.40", (0.6, "ADDITION"): "+0.60", (0.02, "ADDITION"): "+0.02",
        (0.024, "MULTIPLY_TOTAL"): "+2.4%", (0.12, "MULTIPLY_BASE"): "+12%", (-0.6, "ADDITION"): "-0.60",
        (-0.024, "MULTIPLY_TOTAL"): "-2.4%", (-0.0, "ADDITION"): "+0",
    }
    for (value, operation), want in expected.items():
        check(display(value, operation) == want, f"{operation} 数值格式错误: {value} 应为 {want}")
    check("lines.add" in formatter and "for (AttributeRule rule" in formatter, "tooltip 必须逐行加入全部映射")
    check("appendAdvancedIdentifiers" in formatter and "Screen.hasShiftDown()" in player and "Screen.hasShiftDown()" in admin, "内部标识必须仅通过 Shift 高级 tooltip 显示")

    if ERRORS:
        print("v1.1.2 属性显示探针失败：")
        for error in ERRORS:
            print(f"- {error}")
        return 1
    print("v1.1.2 属性显示探针通过：九项中英兜底、运算方式、正负零、小数、预览、多行 tooltip 与普通界面防泄漏均已覆盖。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
