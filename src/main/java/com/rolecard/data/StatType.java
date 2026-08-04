package com.rolecard.data;

import java.util.Arrays;
import java.util.Locale;

public enum StatType {
    BULK("壮硕"), STRENGTH("力量"), COORDINATION("协调"), REFLEX("反应"), SPIRIT("精神"), LUCK("幸运");

    public static final int MIN_VALUE = 0;
    public static final int MAX_VALUE = 100;
    private final String displayName;

    StatType(String displayName) { this.displayName = displayName; }
    public String displayName() { return displayName; }
    public String key() { return name().toLowerCase(Locale.ROOT); }
    public static StatType fromKey(String value) {
        return Arrays.stream(values()).filter(type -> type.key().equals(value.toLowerCase(Locale.ROOT))).findFirst().orElse(null);
    }
    public static int clamp(int value) { return Math.max(MIN_VALUE, Math.min(MAX_VALUE, value)); }
}
