package com.rolecard.data;

import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** 仅保存服务器认可的角色卡；所有外来字段都必须经本类或服务层验证。 */
public final class CharacterCard {
    public static final int DATA_VERSION = 2;
    public static final int MAX_TEXT_LENGTH = 32;
    public static final int MAX_TEXT_UTF8_BYTES = 128;
    public static final int MAX_BIOGRAPHY_LENGTH = 1500;
    public static final int MAX_BIOGRAPHY_UTF8_BYTES = 6000;
    public static final int MAX_AGE = 999;
    private String roleName = "";
    private int age;
    private String gender = "未设定";
    private String biography = "";
    private int availablePoints;
    private long revision;
    private ReviewStatus status = ReviewStatus.DRAFT;
    private String rejectReason = "";
    private long submittedAt;
    private final EnumMap<StatType, Integer> stats = new EnumMap<>(StatType.class);

    public CharacterCard() { resetStats(); }
    public String roleName() { return roleName; }
    public int age() { return age; }
    public String gender() { return gender; }
    public String biography() { return biography; }
    public int availablePoints() { return availablePoints; }
    public long revision() { return revision; }
    public ReviewStatus status() { return status; }
    public String rejectReason() { return rejectReason; }
    public long submittedAt() { return submittedAt; }
    public int stat(StatType type) { return stats.getOrDefault(type, 0); }
    public Map<StatType, Integer> stats() { return Map.copyOf(stats); }
    public String shownName(String fallback) { return roleName.isBlank() ? fallback : roleName; }
    /** 客户端展示的默认可编辑状态；真正规则只在服务端 RoleCardService 中读取配置。 */
    public boolean canPlayerEdit() { return status == ReviewStatus.DRAFT || status == ReviewStatus.REJECTED; }

    public static String sanitizeText(String value, int maxChars, int maxBytes, boolean multiline) {
        if (value == null || value.length() > maxChars || value.getBytes(StandardCharsets.UTF_8).length > maxBytes) return null;
        StringBuilder safe = new StringBuilder();
        for (int i = 0; i < value.length();) {
            int cp = value.codePointAt(i); i += Character.charCount(cp);
            if (cp == '\u00a7' || Character.getType(cp) == Character.FORMAT || Character.isSurrogate((char) cp) || Character.isISOControl(cp) && (!multiline || (cp != '\n' && cp != '\r' && cp != '\t')) || !Character.isValidCodePoint(cp)) return null;
            safe.appendCodePoint(cp);
        }
        String output = safe.toString().replace("\r\n", "\n").replace('\r', '\n').replace("\t", "    ");
        return output;
    }

    public boolean setIdentity(String name, int newAge, String newGender) {
        if (newAge < 0 || newAge > MAX_AGE) return false;
        String safeName = sanitizeText(name, MAX_TEXT_LENGTH, MAX_TEXT_UTF8_BYTES, false);
        String safeGender = sanitizeText(newGender, MAX_TEXT_LENGTH, MAX_TEXT_UTF8_BYTES, false);
        if (safeName == null || safeGender == null) return false;
        roleName = safeName.trim(); age = newAge; gender = safeGender.trim().isBlank() ? "未设定" : safeGender.trim(); return true;
    }
    public boolean setBiography(String value) {
        String safe = sanitizeText(value, MAX_BIOGRAPHY_LENGTH, MAX_BIOGRAPHY_UTF8_BYTES, true);
        if (safe == null) return false;
        biography = safe; return true;
    }
    public void setStat(StatType type, int value) { stats.put(type, StatType.clamp(value)); }
    public void addStat(StatType type, int amount) { setStat(type, stat(type) + amount); }
    public boolean adjustPlayerStat(StatType type, int delta) {
        if (type == null || delta == 0 || Math.abs(delta) > 100) return false;
        int next = stat(type) + delta;
        if (next < 0 || next > 100 || (delta > 0 && availablePoints < delta)) return false;
        setStat(type, next); availablePoints -= delta; touch(); return true;
    }
    public void setAvailablePoints(int value) { availablePoints = Math.max(0, Math.min(100000, value)); touch(); }
    public void addAvailablePoints(int value) { setAvailablePoints(availablePoints + value); }
    public void setStatus(ReviewStatus value, String reason) { status = value == null ? ReviewStatus.DRAFT : value; rejectReason = status == ReviewStatus.REJECTED ? safeReason(reason) : ""; touch(); }
    public void markSubmitted(long time) { submittedAt = Math.max(0, time); }
    public void touch() { revision = revision == Long.MAX_VALUE ? 1 : revision + 1; }
    public void resetStats() { for (StatType type : StatType.values()) stats.put(type, 0); }
    public void resetAll() { roleName = ""; age = 0; gender = "未设定"; biography = ""; availablePoints = 0; status = ReviewStatus.DRAFT; rejectReason = ""; submittedAt = 0; resetStats(); touch(); }
    private static String safeReason(String value) { String safe = sanitizeText(value == null ? "" : value, 160, 640, true); return safe == null ? "审核意见无效" : safe; }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag(); tag.putInt("version", DATA_VERSION); tag.putString("roleName", roleName); tag.putInt("age", age); tag.putString("gender", gender); tag.putString("biography", biography); tag.putInt("availablePoints", availablePoints); tag.putLong("revision", revision); tag.putString("status", status.name()); tag.putString("rejectReason", rejectReason); tag.putLong("submittedAt", submittedAt);
        CompoundTag statTag = new CompoundTag(); for (StatType type : StatType.values()) statTag.putInt(type.key(), stat(type)); tag.put("stats", statTag); return tag;
    }
    public void load(CompoundTag tag) {
        roleName = ""; age = 0; gender = "未设定"; biography = ""; availablePoints = 0; revision = 0; status = ReviewStatus.DRAFT; rejectReason = ""; submittedAt = 0; resetStats(); if (tag == null) return;
        String name = tag.contains("roleName", Tag.TAG_STRING) ? tag.getString("roleName") : ""; String savedGender = tag.contains("gender", Tag.TAG_STRING) ? tag.getString("gender") : "未设定"; int savedAge = tag.contains("age", Tag.TAG_INT) ? tag.getInt("age") : 0;
        if (!setIdentity(name, Math.max(0, Math.min(MAX_AGE, savedAge)), savedGender)) { roleName = ""; age = 0; gender = "未设定"; }
        if (tag.contains("biography", Tag.TAG_STRING)) setBiography(tag.getString("biography"));
        if (tag.contains("stats", Tag.TAG_COMPOUND)) { CompoundTag statTag = tag.getCompound("stats"); for (StatType type : StatType.values()) if (statTag.contains(type.key(), Tag.TAG_INT)) setStat(type, statTag.getInt(type.key())); }
        availablePoints = tag.contains("availablePoints", Tag.TAG_INT) ? Math.max(0, Math.min(100000, tag.getInt("availablePoints"))) : 0;
        revision = tag.contains("revision", Tag.TAG_LONG) ? Math.max(0, tag.getLong("revision")) : 0;
        // v1 没有审核状态。已有管理员六维的旧卡默认锁定，避免升级后可先减点再重分配；
        // 只有身份资料/全零六维的旧卡仍作为草稿，保持 v1 的填写体验。
        boolean hasSavedStatus = tag.contains("status", Tag.TAG_STRING);
        boolean hasLegacyStats = stats.values().stream().anyMatch(value -> value > 0);
        status = hasSavedStatus ? ReviewStatus.fromSaved(tag.getString("status")) : hasLegacyStats ? ReviewStatus.APPROVED : ReviewStatus.DRAFT;
        rejectReason = tag.contains("rejectReason", Tag.TAG_STRING) ? safeReason(tag.getString("rejectReason")) : "";
        submittedAt = tag.contains("submittedAt", Tag.TAG_LONG) ? Math.max(0, tag.getLong("submittedAt")) : 0;
    }
    public void copyFrom(CharacterCard other) { load(other.save()); }
}
