package com.rolecard.mission;

import java.util.Locale;

/** 世界公告的展示状态；不表示自动任务进度。 */
public enum MissionStatus {
    NOT_OPEN("not_open"),
    ACTIVE("active"),
    COMPLETED("completed"),
    CLOSED("closed");

    private final String key;
    MissionStatus(String key) { this.key = key; }
    public String key() { return key; }
    public String translationKey() { return "rolecard.mission.status." + key; }
    public static MissionStatus fromStored(String value) {
        if (value == null) return NOT_OPEN;
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("draft".equals(normalized) || "not_open".equals(normalized)) return NOT_OPEN;
        for (MissionStatus status : values()) if (status.key.equals(normalized)) return status;
        return NOT_OPEN;
    }
}
