package com.rolecard.attribute;

import com.rolecard.data.CharacterCard;
import com.rolecard.data.StatType;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class AttributeMappings {
    private static final Map<StatType, List<AttributeRule>> RULES = Map.of(
            StatType.BULK, List.of(
                    AttributeRule.of("bulk_max_health", () -> Attributes.MAX_HEALTH, AttributeModifier.Operation.ADDITION, 0.2, 100),
                    AttributeRule.of("bulk_armor", () -> Attributes.ARMOR, AttributeModifier.Operation.ADDITION, 0.05, 100),
                    AttributeRule.of("bulk_knockback", () -> Attributes.KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADDITION, 0.002, 100)),
            StatType.STRENGTH, List.of(
                    AttributeRule.of("strength_damage", () -> Attributes.ATTACK_DAMAGE, AttributeModifier.Operation.ADDITION, 0.1, 100),
                    AttributeRule.of("strength_knockback", () -> Attributes.ATTACK_KNOCKBACK, AttributeModifier.Operation.ADDITION, 0.01, 100)),
            StatType.COORDINATION, List.of(AttributeRule.of("coordination_speed", () -> Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.MULTIPLY_TOTAL, 0.002, 100)),
            StatType.REFLEX, List.of(
                    AttributeRule.of("reflex_attack_speed", () -> Attributes.ATTACK_SPEED, AttributeModifier.Operation.MULTIPLY_TOTAL, 0.01, 100),
                    AttributeRule.of("reflex_speed", () -> Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.MULTIPLY_TOTAL, 0.001, 100)),
            StatType.SPIRIT, List.of(
                    AttributeRule.of("spirit_toughness", () -> Attributes.ARMOR_TOUGHNESS, AttributeModifier.Operation.ADDITION, 0.05, 100),
                    AttributeRule.of("spirit_luck", () -> Attributes.LUCK, AttributeModifier.Operation.ADDITION, 0.02, 100)),
            StatType.LUCK, List.of(AttributeRule.of("luck", () -> Attributes.LUCK, AttributeModifier.Operation.ADDITION, 0.1, 100)));

    public static List<AttributeRule> rules(StatType type) { return RULES.getOrDefault(type, List.of()); }
    public static double amount(CharacterCard card, StatType type, AttributeRule rule) { return Math.min(card.stat(type), rule.cap()) * rule.coefficient(); }
    private AttributeMappings() {}
}
