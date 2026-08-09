package com.rolecard.client;

import com.rolecard.data.CharacterCard;
import com.rolecard.data.ReviewStatus;
import com.rolecard.data.StatType;
import com.rolecard.network.AdjustPointsPacket;
import com.rolecard.network.RoleCardNetwork;
import com.rolecard.network.SaveDraftPacket;
import com.rolecard.network.SubmitCardPacket;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** 玩家档案：布局只来自 ArchiveLayout，所有写入仍提交给服务端权威校验。 */
public final class RoleCardScreen extends Screen {
    private ArchiveEditBox nameBox, ageBox, genderBox;
    private MultiLineBiographyBox bioBox;
    private ArchiveButton[] tabs, minus, plus;
    private ArchiveButton close, save, submit;
    private ArchiveLayout.Frame layout;
    private final int initialPage;
    private int page, statsScroll, missionScroll, feedbackTicks;
    private final int[] pendingChanges = new int[StatType.values().length];
    private long observedRevision;
    private String feedback = "";
    private int feedbackColor = ArchiveUi.WARNING;

    public RoleCardScreen() { this(0); }
    /** 用于公告通知与 /rolecard mission view，直接落在只读任务页。 */
    public RoleCardScreen(int initialPage) { super(Component.translatable("rolecard.archive.title")); this.initialPage = Math.max(0, Math.min(3, initialPage)); }
    @Override protected void init() {
        layout = ArchiveLayout.frame(width, height);
        CharacterCard card = ClientCardCache.card(); observedRevision = card.revision();
        ArchiveLayout.Rect content = layout.content();
        nameBox = field(content.x() + 48, content.y() + 16, content.width() - 48, "角色名称", card.roleName(), CharacterCard.MAX_TEXT_LENGTH);
        int half = (content.width() - 4) / 2;
        ageBox = field(content.x(), content.y() + 52, half, "年龄", String.valueOf(card.age()), 3);
        genderBox = field(content.x() + half + 4, content.y() + 52, content.width() - half - 4, "性别", card.gender(), CharacterCard.MAX_TEXT_LENGTH);
        bioBox = new MultiLineBiographyBox(font, content.x(), content.y() + 16, content.width(), Math.max(34, content.height() - 33), Component.literal("输入人物生平；支持换行和滚轮滚动"));
        bioBox.setValue(card.biography()); bioBox.setMaxLength(CharacterCard.MAX_BIOGRAPHY_LENGTH);
        addRenderableWidget(nameBox); addRenderableWidget(ageBox); addRenderableWidget(genderBox); addRenderableWidget(bioBox);
        tabs = new ArchiveButton[4]; Component[] tabNames = {Component.literal("身份"), Component.literal("生平"), Component.literal("六维"), Component.translatable("rolecard.mission.tab")};
        for (int i = 0; i < tabs.length; i++) { final int target = i; ArchiveLayout.Rect r = layout.tab(i, tabs.length); tabs[i] = ArchiveButton.create(tabNames[i], b -> showPage(target), r.x(), r.y(), r.width(), ArchiveUi.ACCENT); addRenderableWidget(tabs[i]); }
        ArchiveLayout.PlayerActions actions = ArchiveLayout.playerActions(layout);
        close = ArchiveButton.create(Component.literal("关闭"), b -> onClose(), actions.close().x(), actions.close().y(), actions.close().width(), ArchiveUi.MUTED);
        submit = ArchiveButton.create(Component.literal("提交角色卡"), b -> submit(), actions.submit().x(), actions.submit().y(), actions.submit().width(), ArchiveUi.ACCENT);
        save = ArchiveButton.create(Component.literal("保存草稿"), b -> save(), actions.save().x(), actions.save().y(), actions.save().width(), ArchiveUi.WARNING);
        addRenderableWidget(close); addRenderableWidget(save); addRenderableWidget(submit);
        minus = new ArchiveButton[StatType.values().length]; plus = new ArchiveButton[StatType.values().length];
        for (int i = 0; i < StatType.values().length; i++) { final int index = i; minus[i] = ArchiveButton.create(Component.literal("−"), b -> adjust(StatType.values()[index], -1), 0, 0, 22, ArchiveUi.MUTED); plus[i] = ArchiveButton.create(Component.literal("+"), b -> adjust(StatType.values()[index], 1), 0, 0, 22, ArchiveUi.ACCENT); addRenderableWidget(minus[i]); addRenderableWidget(plus[i]); }
        showPage(initialPage);
    }
    private ArchiveEditBox field(int x, int y, int w, String hint, String value, int max) { ArchiveEditBox box = new ArchiveEditBox(font, x, y, Math.max(30, w), Component.literal(hint)); box.setValue(value); box.setMaxLength(max); return box; }
    private void showPage(int value) { page = value; statsScroll = 0; missionScroll = 0; updateWidgetState(); }
    private boolean editable() { return ClientCardCache.card().canPlayerEdit(); }
    private void updateWidgetState() {
        if (tabs == null) return;
        boolean edit = editable();
        nameBox.visible = page == 0 && edit; ageBox.visible = page == 0 && edit; genderBox.visible = page == 0 && edit;
        bioBox.visible = page == 1 && edit;
        for (int i = 0; i < tabs.length; i++) tabs[i].active = i != page;
        save.active = edit; submit.active = edit;
        Component reason = Component.literal(edit ? "" : lockedReason()); save.setTooltip(edit ? null : Tooltip.create(reason)); submit.setTooltip(edit ? null : Tooltip.create(reason));
        positionStats();
    }
    private String lockedReason() { return switch (ClientCardCache.card().status()) { case PENDING -> "待审核期间不能提交；请等待管理员处理。"; case APPROVED -> "角色卡已批准并锁定。"; default -> "当前状态不允许提交。"; }; }
    private void save() { try { RoleCardNetwork.CHANNEL.sendToServer(new SaveDraftPacket(ClientCardCache.card().revision(), nameBox.getValue(), Integer.parseInt(ageBox.getValue()), genderBox.getValue(), bioBox.getValue())); notice("已发送保存请求，等待服务器确认。", ArchiveUi.SUCCESS); } catch (NumberFormatException ignored) { notice("年龄必须是 0 至 999 的整数。", ArchiveUi.ERROR); } }
    private void submit() { if (!editable()) { notice(lockedReason(), ArchiveUi.WARNING); return; } RoleCardNetwork.CHANNEL.sendToServer(new SubmitCardPacket(ClientCardCache.card().revision())); notice("已发送提交请求，等待服务器确认。", ArchiveUi.SUCCESS); }
    private void adjust(StatType type, int delta) { if (!editable()) { notice(lockedReason(), ArchiveUi.WARNING); return; } pendingChanges[type.ordinal()] += delta; RoleCardNetwork.CHANNEL.sendToServer(new AdjustPointsPacket(ClientCardCache.card().revision(), type.key(), delta)); }
    private void notice(String text, int color) { feedback = text; feedbackColor = color; feedbackTicks = 100; }
    @Override public void tick() { super.tick(); if (feedbackTicks > 0) feedbackTicks--; }
    @Override public boolean keyPressed(int key, int scan, int modifiers) { if (bioBox.isFocused() && key == 257) { bioBox.insertText("\n"); return true; } return super.keyPressed(key, scan, modifiers); }
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (page == 2 && layout.content().x() <= mouseX && mouseX < layout.content().right()) { statsScroll = Math.max(0, Math.min(statsMaxScroll(), statsScroll - (int)Math.signum(delta) * 16)); positionStats(); return true; }
        if (page == 3 && layout.content().x() <= mouseX && mouseX < layout.content().right()) { missionScroll = Math.max(0, Math.min(missionMaxScroll(), missionScroll - (int)Math.signum(delta) * 16)); return true; }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    private boolean twoColumns() { return layout.content().width() >= 470; }
    private int statCardHeight() { return 38; }
    private int statsMaxScroll() { int rows = twoColumns() ? 3 : 6; return Math.max(0, rows * (statCardHeight() + 4) - 4 - Math.max(1, layout.content().height() - 25)); }
    private void positionStats() {
        if (minus == null) return;
        ArchiveLayout.Rect content = layout.content(); int cols = twoColumns() ? 2 : 1, gap = 4, cardW = (content.width() - gap * (cols - 1)) / cols;
        for (int i = 0; i < minus.length; i++) { int col = i % cols, row = i / cols, x = content.x() + col * (cardW + gap), y = content.y() + 25 + row * (statCardHeight() + 4) - statsScroll; boolean visible = page == 2 && y >= content.y() && y + statCardHeight() <= content.bottom(); minus[i].setX(x + cardW - 48); minus[i].setY(y + 14); plus[i].setX(x + cardW - 24); plus[i].setY(y + 14); minus[i].visible = visible; plus[i].visible = visible; minus[i].active = visible && editable(); plus[i].active = visible && editable(); }
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g); CharacterCard card = ClientCardCache.card(); if (card.revision() != observedRevision) { java.util.Arrays.fill(pendingChanges, 0); observedRevision = card.revision(); updateWidgetState(); notice("资料已从服务器同步，请确认后继续操作。", ArchiveUi.WARNING); }
        ArchiveUi.panel(g, layout.panel()); ArchiveUi.header(g, font, layout.header(), Component.translatable("rolecard.archive.title"), Component.literal("修订 #" + card.revision()));
        int statusW = font.width(card.status().displayName()) + 8; ArchiveUi.badge(g, font, card.status().displayName(), layout.header().right() - statusW - 70, layout.header().y() + 7, ArchiveUi.statusColor(card.status()));
        ArchiveUi.section(g, layout.content());
        List<Component> tooltip = null;
        if (page == 0) renderIdentity(g, card); else if (page == 1) renderBiography(g, card); else if (page == 2) tooltip = renderStats(g, mouseX, mouseY, card); else renderMission(g);
        renderFeedback(g, card); super.render(g, mouseX, mouseY, partial);
        if (tooltip != null) g.renderComponentTooltip(font, tooltip, mouseX, mouseY);
    }
    private void renderIdentity(GuiGraphics g, CharacterCard c) {
        ArchiveLayout.Rect area = ArchiveLayout.inset(layout.content(), 8); ArchiveUi.label(g, font, "身份资料", area.x(), area.y());
        if (editable()) { ArchiveUi.label(g, font, "名称", area.x(), area.y() + 18); ArchiveUi.label(g, font, "年龄", area.x(), area.y() + 54); ArchiveUi.label(g, font, "性别", area.x() + (area.width() + 4) / 2, area.y() + 54); }
        else { readLine(g, "名称", c.roleName().isBlank() ? "未填写" : c.roleName(), area.x(), area.y() + 20); readLine(g, "年龄", String.valueOf(c.age()), area.x(), area.y() + 37); readLine(g, "性别", c.gender(), area.x(), area.y() + 54); }
    }
    private void renderBiography(GuiGraphics g, CharacterCard c) {
        ArchiveLayout.Rect area = ArchiveLayout.inset(layout.content(), 8); ArchiveUi.label(g, font, editable() ? "人物生平（支持换行与滚轮）" : "人物生平（只读）", area.x(), area.y());
        if (editable()) ArchiveUi.label(g, font, bioBox.getValue().length() + " / " + CharacterCard.MAX_BIOGRAPHY_LENGTH + " 字符", area.x(), layout.content().bottom() - 12);
        else readMultiline(g, c.biography().isBlank() ? "未填写生平。" : c.biography(), new ArchiveLayout.Rect(area.x(), area.y() + 16, area.width(), Math.max(1, area.height() - 16)));
    }
    private List<Component> renderStats(GuiGraphics g, int mouseX, int mouseY, CharacterCard c) {
        ArchiveLayout.Rect content = layout.content(); ArchiveUi.badge(g, font, "剩余 " + c.availablePoints() + " 点", content.x() + 8, content.y() + 5, ArchiveUi.ACCENT); ArchiveUi.label(g, font, "悬停属性卡查看实际增益", content.x() + 94, content.y() + 8);
        List<Component> hoverTooltip = null;
        ArchiveUi.clip(g, content); int cols = twoColumns() ? 2 : 1, gap = 4, cardW = (content.width() - gap * (cols - 1)) / cols;
        for (int i = 0; i < StatType.values().length; i++) {
            StatType stat = StatType.values()[i]; int x = content.x() + (i % cols) * (cardW + gap), y = content.y() + 25 + (i / cols) * (statCardHeight() + 4) - statsScroll;
            ArchiveLayout.Rect r = new ArchiveLayout.Rect(x, y, cardW, statCardHeight()); if (r.bottom() <= content.y() || r.y() >= content.bottom()) continue;
            int current = c.stat(stat), preview = StatType.clamp(current + pendingChanges[i]);
            ArchiveUi.section(g, r); g.drawString(font, stat.displayName(), x + 5, y + 5, ArchiveUi.TITLE, false);
            Component value = current == preview ? Component.literal(String.valueOf(current)) : Component.translatable("rolecard.attribute.stat.preview", current, preview);
            g.drawString(font, value, x + cardW - 5 - font.width(value), y + 5, ArchiveUi.SUCCESS, false);
            Component summary = AttributeDisplayFormatter.summary(stat, preview);
            if (current != preview) summary = Component.translatable("rolecard.attribute.summary.preview", summary);
            g.drawString(font, summary, x + 5, y + 18, ArchiveUi.MUTED, false);
            if (mouseX >= r.x() && mouseX < r.right() && mouseY >= r.y() && mouseY < r.bottom()) hoverTooltip = AttributeDisplayFormatter.tooltip(stat, current, preview, Screen.hasShiftDown());
        }
        ArchiveUi.noClip(g);
        return hoverTooltip;
    }
    /** 任务只读页：正文全部在内容视窗裁剪并滚动，避免小分辨率把底栏挤出安全区。 */
    private void renderMission(GuiGraphics g) {
        ClientMissionCache.MissionView mission = ClientMissionCache.mission();
        ArchiveLayout.Rect area = ArchiveLayout.inset(layout.content(), 6);
        if (!mission.published()) {
            int y = area.y() + Math.max(8, area.height() / 2 - 9);
            g.drawCenteredString(font, Component.translatable("rolecard.mission.empty"), area.x() + area.width() / 2, y, ArchiveUi.TITLE);
            g.drawCenteredString(font, Component.translatable("rolecard.mission.empty_hint"), area.x() + area.width() / 2, y + 16, ArchiveUi.MUTED);
            return;
        }
        ArchiveUi.clip(g, area);
        int y = area.y() - missionScroll;
        String status = missionStatus(mission.status());
        ArchiveUi.badge(g, font, status, area.x(), y, missionStatusColor(mission.status()));
        g.drawString(font, mission.title(), area.x(), y + 19, ArchiveUi.TITLE, false); y += 32;
        y = renderMissionMeta(g, mission, area, y);
        y = renderMissionBlock(g, Component.translatable("rolecard.mission.summary").getString(), mission.summary(), area, y, ArchiveUi.TEXT);
        y = renderObjectives(g, mission, area, y);
        y = renderMissionBlock(g, Component.translatable("rolecard.mission.rules").getString(), mission.rules(), area, y, ArchiveUi.TEXT);
        y = renderMissionBlock(g, Component.translatable("rolecard.mission.notes").getString(), mission.notes(), area, y, ArchiveUi.MUTED);
        String audit = Component.translatable("rolecard.mission.audit", mission.revision(), mission.updatedAt() <= 0 ? "—" : new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date(mission.updatedAt())), mission.editorSummary().isBlank() ? "—" : mission.editorSummary()).getString();
        for (net.minecraft.util.FormattedCharSequence line : font.split(Component.literal(audit), area.width())) { g.drawString(font, line, area.x(), y, ArchiveUi.MUTED, false); y += 9; }
        ArchiveUi.noClip(g);
    }
    private int renderMissionMeta(GuiGraphics g, ClientMissionCache.MissionView mission, ArchiveLayout.Rect area, int y) {
        String[] labels = {Component.translatable("rolecard.mission.instance").getString(), Component.translatable("rolecard.mission.difficulty").getString(), Component.translatable("rolecard.mission.players").getString(), Component.translatable("rolecard.mission.time_limit").getString()};
        String instance = mission.instanceName().isBlank() ? "—" : mission.instanceName() + (mission.instanceCode().isBlank() ? "" : " · " + mission.instanceCode());
        String[] values = {instance, emptyDash(mission.difficulty()), emptyDash(mission.playerCountText()), emptyDash(mission.timeLimitText())};
        int cols = area.width() >= 400 ? 2 : 1, cellW = (area.width() - (cols - 1) * 4) / cols;
        for (int i = 0; i < labels.length; i++) {
            int x = area.x() + (i % cols) * (cellW + 4), rowY = y + (i / cols) * 25;
            ArchiveUi.section(g, new ArchiveLayout.Rect(x, rowY, cellW, 22));
            g.drawString(font, labels[i], x + 4, rowY + 3, ArchiveUi.MUTED, false);
            String value = trimToWidth(values[i], cellW - 8);
            g.drawString(font, value, x + 4, rowY + 12, ArchiveUi.TEXT, false);
        }
        return y + ((labels.length + cols - 1) / cols) * 25 + 3;
    }
    private int renderMissionBlock(GuiGraphics g, String heading, String text, ArchiveLayout.Rect area, int y, int color) {
        ArchiveUi.label(g, font, heading, area.x(), y); y += 11;
        String display = text.isBlank() ? Component.translatable("rolecard.mission.none").getString() : text;
        for (net.minecraft.util.FormattedCharSequence line : font.split(Component.literal(display), area.width())) { g.drawString(font, line, area.x() + 2, y, color, false); y += 9; }
        return y + 5;
    }
    private int renderObjectives(GuiGraphics g, ClientMissionCache.MissionView mission, ArchiveLayout.Rect area, int y) {
        ArchiveUi.label(g, font, Component.translatable("rolecard.mission.objectives").getString(), area.x(), y); y += 11;
        if (mission.objectives().isEmpty()) return renderMissionBlock(g, "", "", area, y - 11, ArchiveUi.MUTED);
        for (int i = 0; i < mission.objectives().size(); i++) {
            ClientMissionCache.Objective objective = mission.objectives().get(i);
            String prefix = objective.completed() ? "✓ " : "○ "; int color = objective.completed() ? ArchiveUi.MUTED : ArchiveUi.TEXT;
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal((i + 1) + ". " + prefix + objective.text()), Math.max(1, area.width() - 4));
            for (net.minecraft.util.FormattedCharSequence line : lines) { g.drawString(font, line, area.x() + 2, y, color, false); y += 9; }
            y += 2;
        }
        return y + 3;
    }
    private int missionMaxScroll() {
        ClientMissionCache.MissionView mission = ClientMissionCache.mission(); if (!mission.published()) return 0;
        ArchiveLayout.Rect area = ArchiveLayout.inset(layout.content(), 6); int cols = area.width() >= 400 ? 2 : 1;
        int height = 32 + ((4 + cols - 1) / cols) * 25 + 3;
        height += blockHeight(mission.summary(), area.width()) + 11;
        height += 14; for (ClientMissionCache.Objective item : mission.objectives()) height += font.split(Component.literal(item.text()), Math.max(1, area.width() - 16)).size() * 9 + 2;
        height += blockHeight(mission.rules(), area.width()) + 11 + blockHeight(mission.notes(), area.width()) + 11 + 18;
        return Math.max(0, height - area.height());
    }
    private int blockHeight(String text, int width) { return font.split(Component.literal(text.isBlank() ? Component.translatable("rolecard.mission.none").getString() : text), width).size() * 9 + 5; }
    private String missionStatus(String value) { return Component.translatable("rolecard.mission.status." + value.toLowerCase(java.util.Locale.ROOT)).getString(); }
    private int missionStatusColor(String value) { return switch (value) { case "ACTIVE" -> ArchiveUi.SUCCESS; case "COMPLETED" -> ArchiveUi.ACCENT; case "CLOSED" -> ArchiveUi.ERROR; default -> ArchiveUi.MUTED; }; }
    private String emptyDash(String value) { return value.isBlank() ? "—" : value; }
    private String trimToWidth(String value, int width) { if (font.width(value) <= width) return value; String suffix = "…"; int end = value.length(); while (end > 0 && font.width(value.substring(0, end) + suffix) > width) end--; return value.substring(0, end) + suffix; }
    private void renderFeedback(GuiGraphics g, CharacterCard c) { String message = feedbackTicks > 0 ? feedback : c.status() == ReviewStatus.REJECTED ? "退回原因：" + c.rejectReason() : !editable() ? lockedReason() : ""; int color = feedbackTicks > 0 ? feedbackColor : c.status() == ReviewStatus.REJECTED ? ArchiveUi.ERROR : ArchiveUi.WARNING; if (!message.isEmpty()) g.drawString(font, message, layout.feedback().x(), layout.feedback().y() + 2, color, false); }
    private void readLine(GuiGraphics g, String label, String value, int x, int y) { ArchiveUi.label(g, font, label, x, y); g.drawString(font, value, x + 48, y, ArchiveUi.TEXT, false); }
    private void readMultiline(GuiGraphics g, String text, ArchiveLayout.Rect r) { ArchiveUi.clip(g, r); int y = r.y(); for (net.minecraft.util.FormattedCharSequence line : font.split(Component.literal(text), r.width())) { if (y + 9 > r.bottom()) break; g.drawString(font, line, r.x(), y, ArchiveUi.TEXT, false); y += 9; } ArchiveUi.noClip(g); }
    /** CI 探针入口：构造六维页的原生多行 tooltip 数据，不发送网络包也不绘制屏幕。 */
    public boolean ciInitializeStatsTooltip() { return !AttributeDisplayFormatter.tooltip(StatType.BULK, 12, 13, false).isEmpty(); }
    /** CI 探针入口：仅切换已有页签，不触碰业务数据。 */
    public void ciShowPage(int value) { showPage(Math.max(0, Math.min(3, value))); }
    public boolean ciLayoutWithinSafeArea() { return layout != null && layout.safe().contains(layout.panel()) && !layout.content().intersects(layout.footer()) && !layout.tabs().intersects(layout.content()); }
    @Override public boolean isPauseScreen() { return false; }
}
