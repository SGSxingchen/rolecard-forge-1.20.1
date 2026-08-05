package com.rolecard.service;

import com.rolecard.RoleCardEvents;
import com.rolecard.config.RoleCardConfig;
import com.rolecard.data.CharacterCard;
import com.rolecard.data.ReviewStatus;
import com.rolecard.data.RoleCardCapability;
import com.rolecard.data.StatType;
import com.rolecard.review.ReviewQueueSavedData;
import java.time.Instant;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

/** 所有写入角色卡的入口均经此处，避免命令和网络包绕过 revision/状态校验。 */
public final class RoleCardService {
    public static boolean revisionMatches(CharacterCard card, long baseRevision) { return baseRevision == card.revision(); }
    public static void sync(ServerPlayer player) { RoleCardEvents.syncAndApply(player); }
    public static boolean saveDraft(ServerPlayer player, CharacterCard card, long revision, String name, int age, String gender, String biography) {
        if (!revisionMatches(card, revision) || !canEdit(card)) return false;
        if (biography == null || biography.length() > RoleCardConfig.MAX_BIOGRAPHY_LENGTH.get() || !card.setIdentity(name, age, gender) || !card.setBiography(biography)) return false;
        card.touch(); sync(player); return true;
    }
    public static boolean adjustPoint(ServerPlayer player, CharacterCard card, long revision, StatType stat, int delta) {
        if (!revisionMatches(card, revision) || !canEdit(card) || !card.adjustPlayerStat(stat, delta)) return false;
        sync(player); return true;
    }
    public static boolean submit(ServerPlayer player, CharacterCard card, long revision) {
        if (!revisionMatches(card, revision)) return false;
        if (card.status() == ReviewStatus.PENDING) { // 同版本重放只刷新同一索引，不复制点数或队列条目。
            ReviewQueueSavedData.get(player.server).put(player.getUUID(), player.getGameProfile().getName(), card.shownName(player.getGameProfile().getName()), card.submittedAt());
            sync(player); return true;
        }
        if (!canEdit(card)) return false;
        card.setStatus(ReviewStatus.PENDING, ""); card.markSubmitted(Instant.now().toEpochMilli());
        ReviewQueueSavedData.get(player.server).put(player.getUUID(), player.getGameProfile().getName(), card.shownName(player.getGameProfile().getName()), card.submittedAt());
        sync(player); notifyAdmins(player, card); return true;
    }
    public static boolean adminSave(ServerPlayer target, CharacterCard card, long revision, String name, int age, String gender, String biography, int points, int[] values) {
        if (!revisionMatches(card, revision) || values == null || values.length != StatType.values().length || points < 0 || points > 100000) return false;
        if (biography == null || biography.length() > RoleCardConfig.MAX_BIOGRAPHY_LENGTH.get() || !card.setIdentity(name, age, gender) || !card.setBiography(biography)) return false;
        for (int i = 0; i < values.length; i++) if (values[i] < 0 || values[i] > 100) return false;
        for (int i = 0; i < values.length; i++) card.setStat(StatType.values()[i], values[i]);
        card.setAvailablePoints(points); // 管理员明确设置池，不从客户端派生。
        card.touch(); sync(target); return true;
    }
    public static boolean approve(ServerPlayer target, CharacterCard card) {
        if (card.status() != ReviewStatus.PENDING) return false;
        card.setStatus(ReviewStatus.APPROVED, ""); ReviewQueueSavedData.get(target.server).remove(target.getUUID()); sync(target); target.sendSystemMessage(Component.literal("你的角色卡已获批准，现已锁定。")); return true;
    }
    public static boolean reject(ServerPlayer target, CharacterCard card, String reason) {
        if (card.status() != ReviewStatus.PENDING) return false;
        card.setStatus(ReviewStatus.REJECTED, reason); ReviewQueueSavedData.get(target.server).remove(target.getUUID()); sync(target); target.sendSystemMessage(Component.literal("你的角色卡被退回：" + card.rejectReason() + "。可修改后重新提交。")); return true;
    }
    public static void unlock(ServerPlayer target, CharacterCard card) { card.setStatus(ReviewStatus.DRAFT, ""); ReviewQueueSavedData.get(target.server).remove(target.getUUID()); sync(target); target.sendSystemMessage(Component.literal("管理员已解锁你的角色卡。")); }
    public static boolean canEdit(CharacterCard card) {
        return card.status() == ReviewStatus.DRAFT
                || card.status() == ReviewStatus.PENDING && RoleCardConfig.ALLOW_PENDING_DRAFT_EDITS.get()
                || card.status() == ReviewStatus.REJECTED && RoleCardConfig.ALLOW_REJECTED_EDITS.get();
    }
    public static void notifyAdmins(ServerPlayer subject, CharacterCard card) {
        String command = "/rolecard review open " + subject.getGameProfile().getName();
        MutableComponent open = Component.literal(" [点击审核]").setStyle(Style.EMPTY.withColor(0x55FF55).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)));
        Component message = Component.literal("[角色卡待审核] " + subject.getGameProfile().getName() + "／" + card.shownName(subject.getGameProfile().getName()) + "，提交于 " + Instant.ofEpochMilli(card.submittedAt()) + "，状态：待审核").append(open);
        for (ServerPlayer admin : subject.server.getPlayerList().getPlayers()) if (admin.hasPermissions(2)) admin.sendSystemMessage(message);
    }
    public static void stale(ServerPlayer player) { sync(player); player.sendSystemMessage(Component.literal("角色卡已在其他窗口更新，已同步最新内容；请确认后重试。")); }
    private RoleCardService() {}
}
