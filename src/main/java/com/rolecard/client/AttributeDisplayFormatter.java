package com.rolecard.client;

import com.rolecard.attribute.AttributeMappings;
import com.rolecard.attribute.AttributeRule;
import com.rolecard.data.StatType;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * 角色卡属性的唯一显示入口。此类仅由 client 包使用，翻译在客户端解析，绝不把某种语言预先写进服务端数据。
 */
public final class AttributeDisplayFormatter {
    private static final double ZERO_EPSILON = 0.000_000_1D;
    private static final DecimalFormat FIXED = decimal("0.00#");
    private static final DecimalFormat PERCENT = decimal("0.##");

    private static DecimalFormat decimal(String pattern) {
        DecimalFormat format = new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.ROOT));
        format.setGroupingUsed(false);
        return format;
    }

    public static Component summary(StatType stat, int previewStatValue) {
        List<AttributeRule> rules = AttributeMappings.rules(stat);
        if (rules.isEmpty()) return Component.translatable("rolecard.attribute.none");
        Component first = modifier(rules.get(0), previewStatValue);
        return rules.size() == 1 ? first : Component.translatable("rolecard.attribute.summary.more", first, rules.size());
    }

    public static List<Component> tooltip(StatType stat, int currentStatValue, int previewStatValue, boolean advanced) {
        List<Component> lines = new ArrayList<>();
        Component statName = Component.translatable("rolecard.stat." + stat.key());
        int current = StatType.clamp(currentStatValue), preview = StatType.clamp(previewStatValue);
        lines.add(current == preview
                ? Component.translatable("rolecard.attribute.tooltip.current", statName, current)
                : Component.translatable("rolecard.attribute.tooltip.preview", statName, current, preview));
        for (AttributeRule rule : AttributeMappings.rules(stat)) lines.add(modifier(rule, preview));
        if (advanced) appendAdvancedIdentifiers(lines, stat);
        return lines;
    }

    public static Component modifier(AttributeRule rule, int previewStatValue) {
        return Component.translatable("rolecard.attribute.modifier", attributeName(rule.target().get()), amount(rule, previewStatValue));
    }

    private static Component attributeName(Attribute attribute) {
        String descriptionId = attribute.getDescriptionId();
        if (Language.getInstance().has(descriptionId)) return Component.translatable(descriptionId);
        ResourceLocation id = BuiltInRegistries.ATTRIBUTE.getKey(attribute);
        String mappingKey = id == null ? "" : "rolecard.attribute.name." + id.getNamespace() + "." + id.getPath().replace('/', '.');
        if (!mappingKey.isEmpty() && Language.getInstance().has(mappingKey)) return Component.translatable(mappingKey);
        return Component.literal(friendlyName(id == null ? descriptionId : id.getPath()));
    }

    private static void appendAdvancedIdentifiers(List<Component> lines, StatType stat) {
        lines.add(Component.translatable("rolecard.attribute.tooltip.advanced"));
        for (AttributeRule rule : AttributeMappings.rules(stat)) {
            Attribute attribute = rule.target().get();
            ResourceLocation id = BuiltInRegistries.ATTRIBUTE.getKey(attribute);
            lines.add(Component.translatable("rolecard.attribute.tooltip.identifier", Component.literal(id == null ? "unknown" : id.toString()), Component.literal(attribute.getDescriptionId())));
        }
    }

    private static String amount(AttributeRule rule, int previewStatValue) {
        double value = AttributeMappings.amount(previewStatValue, rule);
        if (Math.abs(value) < ZERO_EPSILON) return "+0";
        String sign = value > 0 ? "+" : "-";
        double magnitude = Math.abs(value);
        return switch (rule.operation()) {
            case ADDITION -> sign + FIXED.format(magnitude);
            case MULTIPLY_BASE, MULTIPLY_TOTAL -> sign + PERCENT.format(magnitude * 100D) + "%";
        };
    }

    private static String friendlyName(String value) {
        String words = value.replaceAll("[._/-]+", " ").trim();
        if (words.isEmpty()) return "Attribute";
        StringBuilder result = new StringBuilder();
        for (String word : words.split("\\s+")) {
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    private AttributeDisplayFormatter() {}
}
