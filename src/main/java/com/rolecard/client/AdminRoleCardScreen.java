package com.rolecard.client;

import com.rolecard.data.CharacterCard;
import com.rolecard.data.StatType;
import com.rolecard.network.AdminReviewActionPacket;
import com.rolecard.network.AdminSaveCardPacket;
import com.rolecard.network.RoleCardNetwork;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** 管理员审核档案；分区避免把长生平、六维与退回原因挤在同一屏。 */
public final class AdminRoleCardScreen extends Screen {
    private final UUID target;
    private final String originalName;
    private final CharacterCard card = new CharacterCard();
    private ArchiveLayout.Frame layout;
    private ArchiveEditBox name, age, gender, points;
    private MultiLineBiographyBox bio, reason;
    private ArchiveEditBox[] stats = new ArchiveEditBox[StatType.values().length];
    private ArchiveButton[] tabs;
    private ArchiveButton close, save, approve, reject, unlock;
    private int page, statsScroll, feedbackTicks;
    private String feedback = ""; private int feedbackColor = ArchiveUi.WARNING;

    public AdminRoleCardScreen(UUID target, String originalName, CompoundTag data) { super(Component.literal("审核档案")); this.target = target; this.originalName = originalName; if (data != null) card.load(data); }
    @Override protected void init() {
        layout = ArchiveLayout.frame(width, height); ArchiveLayout.Rect c = layout.content(); ArchiveLayout.IdentityForm form = ArchiveLayout.identityForm(layout);
        name = field(form.name().field().x(), form.name().field().y(), form.name().field().width(), "角色名称", card.roleName(), CharacterCard.MAX_TEXT_LENGTH);
        age = field(form.age().field().x(), form.age().field().y(), form.age().field().width(), "年龄", String.valueOf(card.age()), 3); gender = field(form.gender().field().x(), form.gender().field().y(), form.gender().field().width(), "性别", card.gender(), CharacterCard.MAX_TEXT_LENGTH);
        int bioY = form.gender().field().bottom() + ArchiveLayout.GAP;
        bio = new MultiLineBiographyBox(font, c.x(), bioY, c.width(), Math.max(1, c.bottom() - bioY), Component.literal("人物生平（支持换行与滚轮）")); bio.setValue(card.biography()); bio.setMaxLength(CharacterCard.MAX_BIOGRAPHY_LENGTH);
        points = field(c.x() + 56, c.y() + 16, 60, "剩余点数", String.valueOf(card.availablePoints()), 6);
        for (int i = 0; i < stats.length; i++) { stats[i] = field(0, 0, 44, StatType.values()[i].displayName(), String.valueOf(card.stat(StatType.values()[i])), 3); addRenderableWidget(stats[i]); }
        reason = new MultiLineBiographyBox(font, c.x(), c.y() + 30, c.width(), Math.max(32, c.height() - 30), Component.literal("退回原因（可选，最多 160 字）")); reason.setMaxLength(160);
        addRenderableWidget(name); addRenderableWidget(age); addRenderableWidget(gender); addRenderableWidget(bio); addRenderableWidget(points); addRenderableWidget(reason);
        tabs = new ArchiveButton[3]; String[] labels = {"资料", "六维", "审核"}; for (int i = 0; i < 3; i++) { final int p = i; ArchiveLayout.Rect r = layout.tab(i); tabs[i] = ArchiveButton.create(Component.literal(labels[i]), b -> showPage(p), r.x(), r.y(), r.width(), ArchiveUi.ACCENT); addRenderableWidget(tabs[i]); }
        ArchiveLayout.AdminActions actions = ArchiveLayout.adminActions(layout);
        close = ArchiveButton.create(Component.literal("关闭"), b -> onClose(), actions.close().x(), actions.close().y(), actions.close().width(), ArchiveUi.MUTED);
        unlock = ArchiveButton.create(Component.literal("解锁"), b -> action("unlock"), actions.unlock().x(), actions.unlock().y(), actions.unlock().width(), ArchiveUi.WARNING);
        approve = ArchiveButton.create(Component.literal("批准"), b -> action("approve"), actions.approve().x(), actions.approve().y(), actions.approve().width(), ArchiveUi.SUCCESS);
        reject = ArchiveButton.create(Component.literal("退回"), b -> action("reject"), actions.reject().x(), actions.reject().y(), actions.reject().width(), ArchiveUi.ERROR);
        save = ArchiveButton.create(Component.literal(actions.save().width() < 72 ? "保存" : "保存调整"), b -> save(), actions.save().x(), actions.save().y(), actions.save().width(), ArchiveUi.ACCENT);
        addRenderableWidget(close); addRenderableWidget(save); addRenderableWidget(reject); addRenderableWidget(approve); addRenderableWidget(unlock); approve.active = card.status() == com.rolecard.data.ReviewStatus.PENDING; reject.active = card.status() == com.rolecard.data.ReviewStatus.PENDING; unlock.active = card.status() != com.rolecard.data.ReviewStatus.DRAFT; showPage(0);
    }
    private ArchiveEditBox field(int x, int y, int width, String hint, String value, int max) { ArchiveEditBox box = new ArchiveEditBox(font, x, y, Math.max(30, width), Component.literal(hint)); box.setValue(value); box.setMaxLength(max); return box; }
    private void showPage(int selected) { page = selected; if (page == 1) statsScroll = 0; boolean details = page == 0, attributes = page == 1; name.visible = details; age.visible = details; gender.visible = details; bio.visible = details; points.visible = attributes; positionStats(attributes); reason.visible = page == 2; for (int i = 0; i < tabs.length; i++) tabs[i].active = i != page; }
    private ArchiveLayout.Rect statsViewport() { ArchiveLayout.Rect c = layout.content(); return new ArchiveLayout.Rect(c.x(), c.y() + 32, c.width(), Math.max(1, c.height() - 32)); }
    private int statsMaxScroll() { return Math.max(0, 3 * 42 - 4 - statsViewport().height()); }
    private void positionStats(boolean visible) { if (layout == null) return; ArchiveLayout.Rect c = layout.content(), viewport = statsViewport(); int cardW = (c.width() - 4) / 2; for (int i = 0; i < stats.length; i++) { int col = i % 2, row = i / 2, x = c.x() + col * (cardW + 4), y = c.y() + 35 + row * 42 - statsScroll; stats[i].setX(x + cardW - 48); stats[i].setY(y + 14); stats[i].visible = visible && y >= viewport.y() && y + 38 <= viewport.bottom(); } }
    private void notice(String text, int color) { feedback = text; feedbackColor = color; feedbackTicks = 100; }
    /** 由仅客户端的反馈 DTO 调用，服务端拒绝不会只落到聊天栏。 */
    public void receiveServerFeedback(String text, boolean error) { notice(text, error ? ArchiveUi.ERROR : ArchiveUi.SUCCESS); }
    @Override public void tick() { super.tick(); if (feedbackTicks > 0) feedbackTicks--; }
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) { if (page == 1 && layout.content().x() <= mouseX && mouseX < layout.content().right()) { statsScroll = Math.max(0, Math.min(statsMaxScroll(), statsScroll - (int)Math.signum(delta) * 16)); positionStats(true); return true; } return super.mouseScrolled(mouseX, mouseY, delta); }
    private void save() { try { int[] values = new int[stats.length]; for (int i = 0; i < values.length; i++) values[i] = Integer.parseInt(stats[i].getValue()); RoleCardNetwork.CHANNEL.sendToServer(new AdminSaveCardPacket(target, card.revision(), name.getValue(), Integer.parseInt(age.getValue()), gender.getValue(), bio.getValue(), Integer.parseInt(points.getValue()), values)); notice("已发送保存调整请求，等待服务器确认。", ArchiveUi.SUCCESS); } catch (NumberFormatException ignored) { notice("年龄、点数和六维必须是有效整数。", ArchiveUi.ERROR); } }
    private void action(String action) { if ("reject".equals(action) && reason.getValue().length() > 160) { notice("退回原因不能超过 160 字。", ArchiveUi.ERROR); return; } RoleCardNetwork.CHANNEL.sendToServer(new AdminReviewActionPacket(target, card.revision(), action, reason.getValue())); notice(("approve".equals(action) ? "批准" : "reject".equals(action) ? "退回" : "解锁") + "请求已发送，等待服务器确认。", "reject".equals(action) ? ArchiveUi.ERROR : ArchiveUi.SUCCESS); }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float tick) {
        renderBackground(g); ArchiveUi.panel(g, layout.panel()); ArchiveUi.header(g, font, layout.header(), Component.literal("审核档案"), Component.literal("修订 #" + card.revision()));
        String status = card.status().displayName(); ArchiveUi.badge(g, font, status, layout.header().right() - font.width(status) - 78, layout.header().y() + 7, ArchiveUi.statusColor(card.status()));
        ArchiveUi.section(g, layout.content()); int x = layout.content().x() + 8, y = layout.content().y() + 5;
        List<Component> tooltip = null;
        if (page == 0) renderDetails(g, x, y); else if (page == 1) tooltip = renderStats(g, x, y, mouseX, mouseY); else renderReview(g, x, y);
        if (feedbackTicks > 0) g.drawString(font, feedback, layout.feedback().x(), layout.feedback().y() + 2, feedbackColor, false);
        super.render(g, mouseX, mouseY, tick);
        if (tooltip != null) g.renderComponentTooltip(font, tooltip, mouseX, mouseY);
    }
    private void renderDetails(GuiGraphics g, int x, int y) { ArchiveLayout.IdentityForm form = ArchiveLayout.identityForm(layout); ArchiveUi.label(g, font, "原版名：" + originalName + "  UUID：" + shortUuid(), x, y); ArchiveUi.label(g, font, "名称", form.name().label().x(), form.name().baseline()); ArchiveUi.label(g, font, "年龄", form.age().label().x(), form.age().baseline()); ArchiveUi.label(g, font, "性别", form.gender().label().x(), form.gender().baseline()); }
    private List<Component> renderStats(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        ArchiveUi.label(g, font, "管理员调整（保存后仍由服务端校验）", x, y); ArchiveUi.label(g, font, "剩余点数", x, y + 18);
        int cols = 2, cardW = (layout.content().width() - 4) / 2; List<Component> tooltip = null; ArchiveLayout.Rect viewport = statsViewport(); ArchiveUi.clip(g, viewport);
        for (int i = 0; i < stats.length; i++) {
            int col = i % cols, row = i / cols, rx = layout.content().x() + col * (cardW + 4), ry = layout.content().y() + 35 + row * 42 - statsScroll;
            StatType stat = StatType.values()[i]; int current = card.stat(stat), preview = previewValue(i, current);
            ArchiveLayout.Rect box = new ArchiveLayout.Rect(rx, ry, cardW, 38); ArchiveUi.section(g, box);
            g.drawString(font, stat.displayName(), rx + 5, ry + 5, ArchiveUi.TITLE, false);
            Component summary = AttributeDisplayFormatter.summary(stat, preview);
            if (current != preview) summary = Component.translatable("rolecard.attribute.summary.preview", summary);
            g.drawString(font, summary, rx + 5, ry + 18, ArchiveUi.MUTED, false);
            if (mouseX >= box.x() && mouseX < box.right() && mouseY >= box.y() && mouseY < box.bottom()) tooltip = AttributeDisplayFormatter.tooltip(stat, current, preview, Screen.hasShiftDown());
        }
        ArchiveUi.noClip(g);
        return tooltip;
    }
    private int previewValue(int index, int current) { try { return StatType.clamp(Integer.parseInt(stats[index].getValue())); } catch (NumberFormatException ignored) { return current; } }
    private void renderReview(GuiGraphics g, int x, int y) { ArchiveUi.label(g, font, "审核状态：" + card.status().displayName(), x, y); g.drawString(font, "退回会通知玩家并允许其按服务器规则修改后重提。", x, y + 14, ArchiveUi.MUTED, false); }
    private String shortUuid() { String value = target.toString(); return value.substring(0, 8) + "…" + value.substring(value.length() - 4); }
    /** CI 探针入口：构造管理员六维的原生多行 tooltip 数据，不发送网络包也不绘制屏幕。 */
    public boolean ciInitializeStatsTooltip() { return !AttributeDisplayFormatter.tooltip(StatType.BULK, 12, 13, false).isEmpty(); }
    /** CI 探针入口：只验证已初始化界面的页签切换与几何关系。 */
    public void ciShowPage(int value) { showPage(Math.max(0, Math.min(2, value))); }
    public boolean ciLayoutWithinSafeArea() { return layout != null && layout.safe().contains(layout.panel()) && !layout.content().intersects(layout.footer()) && !layout.tabs().intersects(layout.content()); }
    @Override public boolean isPauseScreen() { return false; }
}
