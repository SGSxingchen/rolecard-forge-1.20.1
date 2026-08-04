package com.rolecard.data;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public final class CharacterCard {
    public static final int DATA_VERSION = 1;
    public static final int MAX_TEXT_LENGTH = 32;
    public static final int MAX_TEXT_UTF8_BYTES = 128;
    public static final int MAX_AGE = 999;
    private String roleName = "";
    private int age;
    private String gender = "未设定";
    private final EnumMap<StatType, Integer> stats = new EnumMap<>(StatType.class);

    public CharacterCard() { resetStats(); }
    public String roleName() { return roleName; }
    public int age() { return age; }
    public String gender() { return gender; }
    public int stat(StatType type) { return stats.getOrDefault(type, 0); }
    public Map<StatType, Integer> stats() { return Map.copyOf(stats); }
    public String shownName(String fallback) { return roleName.isBlank() ? fallback : roleName; }

    public boolean setIdentity(String name, int newAge, String newGender) {
        if (name == null || newGender == null || newAge < 0 || newAge > MAX_AGE) return false;
        String safeName = name.trim();
        String safeGender = newGender.trim();
        if (safeName.length() > MAX_TEXT_LENGTH || safeGender.length() > MAX_TEXT_LENGTH
                || safeName.indexOf('\u00a7') >= 0 || safeGender.indexOf('\u00a7') >= 0
                || safeName.chars().anyMatch(Character::isISOControl)
                || safeGender.chars().anyMatch(Character::isISOControl)) return false;
        roleName = safeName;
        age = newAge;
        gender = safeGender.isBlank() ? "未设定" : safeGender;
        return true;
    }

    public void setStat(StatType type, int value) { stats.put(type, StatType.clamp(value)); }
    public void addStat(StatType type, int amount) { setStat(type, stat(type) + amount); }
    public void resetStats() { for (StatType type : StatType.values()) stats.put(type, 0); }
    public void resetAll() { roleName = ""; age = 0; gender = "未设定"; resetStats(); }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("version", DATA_VERSION);
        tag.putString("roleName", roleName);
        tag.putInt("age", age);
        tag.putString("gender", gender);
        CompoundTag statTag = new CompoundTag();
        for (StatType type : StatType.values()) statTag.putInt(type.key(), stat(type));
        tag.put("stats", statTag);
        return tag;
    }

    public void load(CompoundTag tag) {
        resetAll();
        if (tag == null) return;
        String name = tag.contains("roleName", Tag.TAG_STRING) ? tag.getString("roleName") : "";
        String gender = tag.contains("gender", Tag.TAG_STRING) ? tag.getString("gender") : "未设定";
        int savedAge = tag.contains("age", Tag.TAG_INT) ? tag.getInt("age") : 0;
        if (!setIdentity(name, Math.max(0, Math.min(MAX_AGE, savedAge)), gender)) resetAll();
        if (!tag.contains("stats", Tag.TAG_COMPOUND)) return;
        CompoundTag statTag = tag.getCompound("stats");
        for (StatType type : StatType.values()) {
            if (statTag.contains(type.key(), Tag.TAG_INT)) setStat(type, statTag.getInt(type.key()));
        }
    }

    public void copyFrom(CharacterCard other) { load(other.save()); }
}
