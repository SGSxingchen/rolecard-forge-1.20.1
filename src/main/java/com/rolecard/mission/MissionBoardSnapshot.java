package com.rolecard.mission;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * 任务公告的跨端 DTO（版本 1）。它只承载显示资料，绝不承担任务判定、奖励或传送。
 * 所有构造结果都会被收敛为安全大小；C2S 写入仍需用 {@link #isValidClientTag(CompoundTag)} 严格校验原始输入。
 */
public record MissionBoardSnapshot(int dataVersion, long revision, String title, String summary,
        List<MissionObjective> objectives, String rules, String notes, String instanceName,
        String instanceCode, String difficulty, String playerCountText, String timeLimitText,
        MissionStatus status, long updatedAt, String lastEditor) {
    public static final int DATA_VERSION = 1;
    public static final int MAX_TITLE_LENGTH = 80;
    public static final int MAX_SUMMARY_LENGTH = 1200;
    public static final int MAX_OBJECTIVES = 12;
    public static final int MAX_OBJECTIVE_LENGTH = 200;
    public static final int MAX_RULES_LENGTH = 1000;
    public static final int MAX_NOTES_LENGTH = 600;
    public static final int MAX_INSTANCE_NAME_LENGTH = 80;
    public static final int MAX_INSTANCE_CODE_LENGTH = 48;
    public static final int MAX_DIFFICULTY_LENGTH = 48;
    public static final int MAX_PLAYER_COUNT_LENGTH = 48;
    public static final int MAX_TIME_LIMIT_LENGTH = 48;
    public static final int MAX_EDITOR_LENGTH = 48;
    public static final int MAX_TEXT_UTF8_BYTES = 4800;

    public MissionBoardSnapshot {
        dataVersion = DATA_VERSION;
        revision = Math.max(0L, revision);
        title = clean(title, MAX_TITLE_LENGTH, false);
        summary = clean(summary, MAX_SUMMARY_LENGTH, true);
        objectives = cleanObjectives(objectives);
        rules = clean(rules, MAX_RULES_LENGTH, true);
        notes = clean(notes, MAX_NOTES_LENGTH, true);
        instanceName = clean(instanceName, MAX_INSTANCE_NAME_LENGTH, false);
        instanceCode = clean(instanceCode, MAX_INSTANCE_CODE_LENGTH, false);
        difficulty = clean(difficulty, MAX_DIFFICULTY_LENGTH, false);
        playerCountText = clean(playerCountText, MAX_PLAYER_COUNT_LENGTH, false);
        timeLimitText = clean(timeLimitText, MAX_TIME_LIMIT_LENGTH, false);
        status = status == null ? MissionStatus.NOT_OPEN : status;
        updatedAt = Math.max(0L, updatedAt);
        lastEditor = clean(lastEditor, MAX_EDITOR_LENGTH, false);
    }

    public static MissionBoardSnapshot empty() {
        return new MissionBoardSnapshot(DATA_VERSION, 0L, "", "", List.of(), "", "", "", "", "", "", "", MissionStatus.NOT_OPEN, 0L, "");
    }
    public static MissionBoardSnapshot empty(long revision, long updatedAt, String editor) {
        return new MissionBoardSnapshot(DATA_VERSION, revision, "", "", List.of(), "", "", "", "", "", "", "", MissionStatus.NOT_OPEN, updatedAt, editor);
    }
    public boolean hasMission() { return !title.isBlank(); }
    public MissionBoardSnapshot withRevision(long nextRevision, long at, String editor) {
        return new MissionBoardSnapshot(DATA_VERSION, nextRevision, title, summary, objectives, rules, notes, instanceName, instanceCode, difficulty, playerCountText, timeLimitText, status, at, editor);
    }
    public MissionBoardSnapshot withTitle(String value) { return copy(value, summary, objectives, rules, notes, instanceName, instanceCode, difficulty, playerCountText, timeLimitText, status); }
    public MissionBoardSnapshot withSummary(String value) { return copy(title, value, objectives, rules, notes, instanceName, instanceCode, difficulty, playerCountText, timeLimitText, status); }
    public MissionBoardSnapshot withInstanceName(String value) { return copy(title, summary, objectives, rules, notes, value, instanceCode, difficulty, playerCountText, timeLimitText, status); }
    public MissionBoardSnapshot withStatus(MissionStatus value) { return copy(title, summary, objectives, rules, notes, instanceName, instanceCode, difficulty, playerCountText, timeLimitText, value); }
    public MissionBoardSnapshot withObjectives(List<MissionObjective> value) { return copy(title, summary, value, rules, notes, instanceName, instanceCode, difficulty, playerCountText, timeLimitText, status); }
    private MissionBoardSnapshot copy(String nextTitle, String nextSummary, List<MissionObjective> nextObjectives, String nextRules, String nextNotes, String nextInstanceName, String nextInstanceCode, String nextDifficulty, String nextPlayerCount, String nextTimeLimit, MissionStatus nextStatus) {
        return new MissionBoardSnapshot(DATA_VERSION, revision, nextTitle, nextSummary, nextObjectives, nextRules, nextNotes, nextInstanceName, nextInstanceCode, nextDifficulty, nextPlayerCount, nextTimeLimit, nextStatus, updatedAt, lastEditor);
    }
    /** 比较发布内容，故意忽略 revision、时间与编辑人。 */
    public boolean sameContent(MissionBoardSnapshot other) {
        return other != null && title.equals(other.title) && summary.equals(other.summary) && objectives.equals(other.objectives)
                && rules.equals(other.rules) && notes.equals(other.notes) && instanceName.equals(other.instanceName)
                && instanceCode.equals(other.instanceCode) && difficulty.equals(other.difficulty)
                && playerCountText.equals(other.playerCountText) && timeLimitText.equals(other.timeLimitText) && status == other.status;
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("dataVersion", DATA_VERSION); tag.putLong("revision", revision);
        tag.putString("title", title); tag.putString("summary", summary); tag.putString("rules", rules); tag.putString("notes", notes);
        tag.putString("instanceName", instanceName); tag.putString("instanceCode", instanceCode); tag.putString("difficulty", difficulty);
        tag.putString("playerCountText", playerCountText); tag.putString("timeLimitText", timeLimitText); tag.putString("status", status.key());
        tag.putLong("updatedAt", updatedAt); tag.putString("lastEditor", lastEditor);
        ListTag list = new ListTag();
        for (MissionObjective objective : objectives) { CompoundTag row = new CompoundTag(); row.putBoolean("completed", objective.completed()); row.putString("text", objective.text()); list.add(row); }
        tag.put("objectives", list);
        return tag;
    }
    public static MissionBoardSnapshot fromTag(CompoundTag tag) {
        if (tag == null) return empty();
        List<MissionObjective> objectives = new ArrayList<>();
        if (tag.contains("objectives", Tag.TAG_LIST)) {
            ListTag rows = tag.getList("objectives", Tag.TAG_COMPOUND);
            for (int i = 0; i < Math.min(rows.size(), MAX_OBJECTIVES); i++) {
                CompoundTag row = rows.getCompound(i);
                objectives.add(new MissionObjective(row.getBoolean("completed"), row.getString("text")));
            }
        }
        return new MissionBoardSnapshot(DATA_VERSION, tag.getLong("revision"), tag.getString("title"), tag.getString("summary"), objectives,
                tag.getString("rules"), tag.getString("notes"), tag.getString("instanceName"), tag.getString("instanceCode"),
                tag.getString("difficulty"), tag.getString("playerCountText"), tag.getString("timeLimitText"), MissionStatus.fromStored(tag.getString("status")),
                tag.getLong("updatedAt"), tag.getString("lastEditor"));
    }
    /** 用于管理员 C2S 包：超限、非法字段类型、控制/格式字符均拒绝，不做静默截断。 */
    public static boolean isValidClientTag(CompoundTag tag) {
        if (tag == null || !tag.contains("objectives", Tag.TAG_LIST)) return false;
        if (!isString(tag, "title") || !isString(tag, "summary") || !isString(tag, "rules") || !isString(tag, "notes")
                || !isString(tag, "instanceName") || !isString(tag, "instanceCode") || !isString(tag, "difficulty")
                || !isString(tag, "playerCountText") || !isString(tag, "timeLimitText") || !isString(tag, "status")) return false;
        if (tag.getList("objectives", Tag.TAG_COMPOUND).size() > MAX_OBJECTIVES) return false;
        if (!isValid(tag.getString("title"), MAX_TITLE_LENGTH, false) || !isValid(tag.getString("summary"), MAX_SUMMARY_LENGTH, true)
                || !isValid(tag.getString("rules"), MAX_RULES_LENGTH, true) || !isValid(tag.getString("notes"), MAX_NOTES_LENGTH, true)
                || !isValid(tag.getString("instanceName"), MAX_INSTANCE_NAME_LENGTH, false) || !isValid(tag.getString("instanceCode"), MAX_INSTANCE_CODE_LENGTH, false)
                || !isValid(tag.getString("difficulty"), MAX_DIFFICULTY_LENGTH, false) || !isValid(tag.getString("playerCountText"), MAX_PLAYER_COUNT_LENGTH, false)
                || !isValid(tag.getString("timeLimitText"), MAX_TIME_LIMIT_LENGTH, false)) return false;
        String status = tag.getString("status");
        if (!status.isEmpty() && MissionStatus.fromStored(status).key().equals("not_open") && !"not_open".equalsIgnoreCase(status) && !"draft".equalsIgnoreCase(status)) return false;
        for (Tag value : tag.getList("objectives", Tag.TAG_COMPOUND)) {
            CompoundTag row = (CompoundTag) value;
            if (!row.contains("completed", Tag.TAG_BYTE) || !isString(row, "text") || !isValid(row.getString("text"), MAX_OBJECTIVE_LENGTH, false)) return false;
        }
        return true;
    }
    private static boolean isString(CompoundTag tag, String key) { return tag.contains(key, Tag.TAG_STRING); }
    public static boolean isValid(String value, int maxChars, boolean multiline) {
        if (value == null || value.length() > maxChars || value.getBytes(StandardCharsets.UTF_8).length > Math.min(MAX_TEXT_UTF8_BYTES, maxChars * 4 + 32)) return false;
        return clean(value, maxChars, multiline).equals(value);
    }
    static String clean(String value, int maxChars, boolean multiline) {
        if (value == null || value.isEmpty()) return "";
        StringBuilder output = new StringBuilder(Math.min(value.length(), maxChars));
        for (int offset = 0; offset < value.length() && output.length() < maxChars;) {
            int codePoint = value.codePointAt(offset); offset += Character.charCount(codePoint);
            if (codePoint == '\u00a7' || Character.getType(codePoint) == Character.FORMAT || Character.isISOControl(codePoint)) {
                if (multiline && (codePoint == '\n' || codePoint == '\t')) output.appendCodePoint(codePoint);
                continue;
            }
            output.appendCodePoint(codePoint);
        }
        String result = output.toString();
        while (result.getBytes(StandardCharsets.UTF_8).length > Math.min(MAX_TEXT_UTF8_BYTES, maxChars * 4 + 32)) result = result.substring(0, result.offsetByCodePoints(result.length(), -1));
        return result;
    }
    private static List<MissionObjective> cleanObjectives(List<MissionObjective> source) {
        if (source == null || source.isEmpty()) return List.of();
        List<MissionObjective> result = new ArrayList<>();
        for (MissionObjective objective : source) { if (objective != null && result.size() < MAX_OBJECTIVES) result.add(new MissionObjective(objective.completed(), objective.text())); }
        return List.copyOf(result);
    }
}
