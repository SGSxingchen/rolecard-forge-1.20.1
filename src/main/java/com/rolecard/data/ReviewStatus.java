package com.rolecard.data;

/** 服务端控制的角色卡审核状态。 */
public enum ReviewStatus {
    DRAFT("草稿"), PENDING("待审核"), APPROVED("已批准"), REJECTED("已退回");
    private final String display;
    ReviewStatus(String display) { this.display = display; }
    public String displayName() { return display; }
    public static ReviewStatus fromSaved(String value) {
        try { return value == null ? DRAFT : valueOf(value); } catch (IllegalArgumentException ignored) { return DRAFT; }
    }
}
