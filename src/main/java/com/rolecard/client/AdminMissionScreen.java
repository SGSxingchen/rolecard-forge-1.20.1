package com.rolecard.client;

import com.rolecard.mission.MissionBoardSnapshot;
import com.rolecard.mission.MissionObjective;
import com.rolecard.mission.MissionStatus;
import com.rolecard.network.AdminSaveMissionPacket;
import com.rolecard.network.RoleCardNetwork;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/**
 * 管理员任务编辑器。目标行控件在 init 时一次性建立；新增、删除、滚动绝不 rebuild Screen，
 * 从而不会留下携带旧索引的 Widget 或失效焦点。
 */
public final class AdminMissionScreen extends Screen {
    /** 只由 CI 探针安装；生产环境始终为空并使用真实 SimpleChannel。 */
    private static Consumer<AdminSaveMissionPacket> ciPacketObserver;
    private final MissionBoardSnapshot original;
    private final List<MissionObjective> objectives = new ArrayList<>();
    private ArchiveLayout.Frame layout;
    private ArchiveButton[] tabs, objectiveToggle, objectiveRemove;
    private ArchiveButton close, publish, statusButton, addObjective;
    private ArchiveEditBox title, instanceName, instanceCode, difficulty, players, timeLimit;
    private ArchiveEditBox[] objectiveText;
    private MultiLineBiographyBox summary, rules, notes;
    private MissionStatus status;
    private int page, basicScroll, objectiveScroll, feedbackTicks;
    private String feedback = ""; private int feedbackColor = ArchiveUi.WARNING;

    public AdminMissionScreen(CompoundTag data) {
        super(Component.translatable("rolecard.mission.admin.title"));
        original = MissionBoardSnapshot.fromTag(data); objectives.addAll(original.objectives()); status = original.status();
    }
    @Override protected void init() {
        layout = ArchiveLayout.frame(width, height); ArchiveLayout.Rect c = layout.content();
        title = field(0, 0, "rolecard.mission.field.title", original.title(), MissionBoardSnapshot.MAX_TITLE_LENGTH);
        instanceName = field(0, 0, "rolecard.mission.field.instance", original.instanceName(), MissionBoardSnapshot.MAX_INSTANCE_NAME_LENGTH);
        instanceCode = field(0, 0, "rolecard.mission.field.instance_code", original.instanceCode(), MissionBoardSnapshot.MAX_INSTANCE_CODE_LENGTH);
        difficulty = field(0, 0, "rolecard.mission.field.difficulty", original.difficulty(), MissionBoardSnapshot.MAX_DIFFICULTY_LENGTH);
        players = field(0, 0, "rolecard.mission.field.players", original.playerCountText(), MissionBoardSnapshot.MAX_PLAYER_COUNT_LENGTH);
        timeLimit = field(0, 0, "rolecard.mission.field.time_limit", original.timeLimitText(), MissionBoardSnapshot.MAX_TIME_LIMIT_LENGTH);
        summary = multiline(c.x(), c.y() + 16, c.width(), Math.max(30, c.height() - 16), "rolecard.mission.field.summary", original.summary(), MissionBoardSnapshot.MAX_SUMMARY_LENGTH);
        int half = Math.max(28, (c.height() - 32) / 2);
        rules = multiline(c.x(), c.y() + 16, c.width(), half, "rolecard.mission.field.rules", original.rules(), MissionBoardSnapshot.MAX_RULES_LENGTH);
        notes = multiline(c.x(), c.y() + 31 + half, c.width(), Math.max(28, c.height() - 31 - half), "rolecard.mission.field.notes", original.notes(), MissionBoardSnapshot.MAX_NOTES_LENGTH);
        addRenderableWidget(title); addRenderableWidget(instanceName); addRenderableWidget(instanceCode); addRenderableWidget(difficulty); addRenderableWidget(players); addRenderableWidget(timeLimit); addRenderableWidget(summary); addRenderableWidget(rules); addRenderableWidget(notes);
        objectiveText = new ArchiveEditBox[MissionBoardSnapshot.MAX_OBJECTIVES]; objectiveToggle = new ArchiveButton[MissionBoardSnapshot.MAX_OBJECTIVES]; objectiveRemove = new ArchiveButton[MissionBoardSnapshot.MAX_OBJECTIVES];
        for (int i = 0; i < MissionBoardSnapshot.MAX_OBJECTIVES; i++) {
            final int index = i;
            objectiveText[i] = field(0, 0, "rolecard.mission.objective_text", i < objectives.size() ? objectives.get(i).text() : "", MissionBoardSnapshot.MAX_OBJECTIVE_LENGTH);
            objectiveToggle[i] = button("✓", b -> toggleObjective(index), 0, 0, 20, ArchiveUi.SUCCESS);
            objectiveRemove[i] = button("×", b -> removeObjective(index), 0, 0, 20, ArchiveUi.ERROR);
            addRenderableWidget(objectiveText[i]); addRenderableWidget(objectiveToggle[i]); addRenderableWidget(objectiveRemove[i]);
        }
        tabs = new ArchiveButton[4]; String[] keys = {"rolecard.mission.admin.basic", "rolecard.mission.summary", "rolecard.mission.objectives", "rolecard.mission.admin.rules_notes"};
        for (int i = 0; i < tabs.length; i++) { final int target = i; ArchiveLayout.Rect r = layout.tab(i, tabs.length); tabs[i] = ArchiveButton.create(Component.translatable(keys[i]), b -> showPage(target), r.x(), r.y(), r.width(), ArchiveUi.ACCENT); addRenderableWidget(tabs[i]); }
        ArchiveLayout.PlayerActions actions = ArchiveLayout.playerActions(layout);
        close = ArchiveButton.create(Component.translatable("rolecard.common.close"), b -> onClose(), actions.close().x(), actions.close().y(), actions.close().width(), ArchiveUi.MUTED);
        publish = ArchiveButton.create(Component.translatable("rolecard.mission.admin.publish"), b -> publish(), actions.submit().x(), actions.submit().y(), actions.submit().width(), ArchiveUi.ACCENT);
        statusButton = ArchiveButton.create(Component.literal(""), b -> { status = MissionStatus.values()[(status.ordinal() + 1) % MissionStatus.values().length]; updateStatusButton(); }, 0, 0, 82, ArchiveUi.WARNING);
        addObjective = button("rolecard.mission.admin.add", b -> addObjective(), 0, 0, 56, ArchiveUi.ACCENT);
        addRenderableWidget(close); addRenderableWidget(publish); addRenderableWidget(statusButton); addRenderableWidget(addObjective);
        showPage(0);
    }
    private ArchiveEditBox field(int x, int y, String key, String value, int max) { ArchiveEditBox box = new ArchiveEditBox(font, x, y, 30, Component.translatable(key)); box.setValue(value); box.setMaxLength(max); return box; }
    private MultiLineBiographyBox multiline(int x, int y, int w, int h, String key, String value, int max) { MultiLineBiographyBox box = new MultiLineBiographyBox(font, x, y, w, h, Component.translatable(key)); box.setValue(value); box.setMaxLength(max); return box; }
    private ArchiveButton button(String text, net.minecraft.client.gui.components.Button.OnPress press, int x, int y, int w, int tone) { return ArchiveButton.create(Component.literal(text), press, x, y, w, tone); }
    private void showPage(int value) { page = Math.max(0, Math.min(3, value)); basicScroll = 0; objectiveScroll = 0; updateVisibility(); }
    private void updateVisibility() {
        boolean basic = page == 0, objective = page == 2;
        title.visible = basic; instanceName.visible = basic; instanceCode.visible = basic; difficulty.visible = basic; players.visible = basic; timeLimit.visible = basic; statusButton.visible = basic;
        summary.visible = page == 1; rules.visible = page == 3; notes.visible = page == 3; addObjective.visible = objective;
        for (int i = 0; i < objectiveText.length; i++) { boolean visible = objective && i < objectives.size(); objectiveText[i].visible = visible; objectiveToggle[i].visible = visible; objectiveRemove[i].visible = visible; }
        for (int i = 0; i < tabs.length; i++) tabs[i].active = i != page;
        positionBasic(); positionObjectives(); updateStatusButton();
    }
    private void positionBasic() {
        if (layout == null) return; ArchiveLayout.Rect c = ArchiveLayout.inset(layout.content(), ArchiveLayout.GAP);
        ArchiveEditBox[] fields = {title, instanceName, instanceCode, difficulty, players, timeLimit};
        for (int i = 0; i < fields.length; i++) { int y = c.y() + 18 + i * 28 - basicScroll; fields[i].setX(c.x()); fields[i].setY(y); fields[i].setWidth(c.width()); fields[i].visible = page == 0 && y >= c.y() && y + ArchiveLayout.CONTROL_HEIGHT <= c.bottom(); }
        int statusY = c.y() + 18 + fields.length * 28 - basicScroll; statusButton.setX(c.x()); statusButton.setY(statusY); statusButton.visible = page == 0 && statusY >= c.y() && statusY + ArchiveLayout.CONTROL_HEIGHT <= c.bottom();
    }
    private int basicMaxScroll() { ArchiveLayout.Rect c = ArchiveLayout.inset(layout.content(), ArchiveLayout.GAP); return Math.max(0, 18 + 6 * 28 + ArchiveLayout.CONTROL_HEIGHT - c.height()); }
    private void positionObjectives() {
        if (layout == null || objectiveText == null) return;
        ArchiveLayout.MissionEditor editor = ArchiveLayout.missionEditor(layout, objectives.size(), objectiveScroll);
        addObjective.setX(editor.add().x()); addObjective.setY(editor.add().y());
        for (int i = 0; i < objectiveText.length; i++) {
            if (i >= editor.rows().size()) { objectiveText[i].visible = false; objectiveToggle[i].visible = false; objectiveRemove[i].visible = false; objectiveText[i].setFocused(false); continue; }
            ArchiveLayout.ObjectiveRow row = editor.rows().get(i); objectiveToggle[i].setX(row.toggle().x()); objectiveToggle[i].setY(row.toggle().y()); objectiveText[i].setX(row.text().x()); objectiveText[i].setY(row.text().y()); objectiveText[i].setWidth(row.text().width()); objectiveRemove[i].setX(row.remove().x()); objectiveRemove[i].setY(row.remove().y()); objectiveToggle[i].setMessage(Component.literal(objectives.get(i).completed() ? "✓" : "○"));
            boolean inViewport = editor.list().contains(row.text()); objectiveText[i].visible = page == 2 && inViewport; objectiveToggle[i].visible = page == 2 && inViewport; objectiveRemove[i].visible = page == 2 && inViewport;
        }
        addObjective.active = objectives.size() < MissionBoardSnapshot.MAX_OBJECTIVES;
    }
    private void updateStatusButton() { statusButton.setMessage(Component.translatable(status.translationKey())); }
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (layout == null || mouseX < layout.content().x() || mouseX >= layout.content().right()) return super.mouseScrolled(mouseX, mouseY, delta);
        if (page == 0) { basicScroll = Math.max(0, Math.min(basicMaxScroll(), basicScroll - (int)Math.signum(delta) * 16)); positionBasic(); return true; }
        if (page == 2) { objectiveScroll = Math.max(0, Math.min(ArchiveLayout.missionObjectiveMaxScroll(layout, objectives.size()), objectiveScroll - (int)Math.signum(delta) * 16)); positionObjectives(); return true; }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    private void commitObjective(int index) { if (index >= 0 && index < objectives.size()) objectives.set(index, new MissionObjective(objectives.get(index).completed(), objectiveText[index].getValue())); }
    private void commitObjectives() { for (int i = 0; i < objectives.size(); i++) commitObjective(i); }
    private void addObjective() {
        commitObjectives();
        if (objectives.size() >= MissionBoardSnapshot.MAX_OBJECTIVES) { notice(Component.translatable("rolecard.mission.admin.max_objectives").getString(), ArchiveUi.ERROR); return; }
        int index = objectives.size(); objectives.add(new MissionObjective(false, "")); objectiveText[index].setValue("");
        objectiveScroll = ArchiveLayout.missionObjectiveMaxScroll(layout, objectives.size()); positionObjectives(); setFocused(objectiveText[index]); notice("已新增目标 " + (index + 1) + "，可直接输入内容。", ArchiveUi.SUCCESS);
    }
    private void removeObjective(int index) {
        commitObjectives(); if (index < 0 || index >= objectives.size()) return;
        objectives.remove(index);
        for (int i = index; i < objectives.size(); i++) objectiveText[i].setValue(objectives.get(i).text());
        if (getFocused() == objectiveText[Math.min(index, objectiveText.length - 1)]) setFocused(null);
        objectiveScroll = Math.min(objectiveScroll, ArchiveLayout.missionObjectiveMaxScroll(layout, objectives.size())); positionObjectives(); notice("已删除目标。", ArchiveUi.WARNING);
    }
    private void toggleObjective(int index) { commitObjective(index); if (index < 0 || index >= objectives.size()) return; MissionObjective old = objectives.get(index); objectives.set(index, new MissionObjective(!old.completed(), old.text())); objectiveToggle[index].setMessage(Component.literal(old.completed() ? "✓" : "○")); }
    private void publish() {
        commitObjectives(); CompoundTag tag = new MissionBoardSnapshot(MissionBoardSnapshot.DATA_VERSION, original.revision(), title.getValue(), summary.getValue(), List.copyOf(objectives), rules.getValue(), notes.getValue(), instanceName.getValue(), instanceCode.getValue(), difficulty.getValue(), players.getValue(), timeLimit.getValue(), status, 0L, "").toTag();
        AdminSaveMissionPacket packet = new AdminSaveMissionPacket(original.revision(), tag);
        if (ciPacketObserver != null) ciPacketObserver.accept(packet); else RoleCardNetwork.CHANNEL.sendToServer(packet);
        notice(Component.translatable("rolecard.mission.admin.sending").getString(), ArchiveUi.SUCCESS);
    }
    public void receiveServerFeedback(String text, boolean error) { notice(text, error ? ArchiveUi.ERROR : ArchiveUi.SUCCESS); }
    private void notice(String text, int color) { feedback = text; feedbackColor = color; feedbackTicks = 100; }
    @Override public void tick() { super.tick(); if (feedbackTicks > 0) feedbackTicks--; }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float tick) {
        renderBackground(g); ArchiveUi.panel(g, layout.panel()); ArchiveUi.header(g, font, layout.header(), Component.translatable("rolecard.mission.admin.title"), Component.translatable(status.translationKey())); ArchiveUi.section(g, layout.content());
        ArchiveLayout.Rect c = layout.content(); ArchiveUi.clip(g, c);
        if (page == 0) renderBasic(g, c); else if (page == 1) renderTextPage(g, Component.translatable("rolecard.mission.summary"), summary.getValue().length(), MissionBoardSnapshot.MAX_SUMMARY_LENGTH, c); else if (page == 2) renderObjectives(g); else renderRules(g, c);
        ArchiveUi.noClip(g); if (feedbackTicks > 0) g.drawString(font, feedback, layout.feedback().x(), layout.feedback().y() + 2, feedbackColor, false); super.render(g, mouseX, mouseY, tick);
    }
    private void renderBasic(GuiGraphics g, ArchiveLayout.Rect c) { String[] keys = {"rolecard.mission.field.title", "rolecard.mission.field.instance", "rolecard.mission.field.instance_code", "rolecard.mission.field.difficulty", "rolecard.mission.field.players", "rolecard.mission.field.time_limit"}; for (int i = 0; i < keys.length; i++) ArchiveUi.label(g, font, Component.translatable(keys[i]).getString(), c.x() + ArchiveLayout.GAP, c.y() + ArchiveLayout.GAP + i * 28 - basicScroll); ArchiveUi.label(g, font, Component.translatable("rolecard.mission.field.status").getString(), c.x() + ArchiveLayout.GAP, c.y() + ArchiveLayout.GAP + keys.length * 28 - basicScroll); }
    private void renderTextPage(GuiGraphics g, Component heading, int length, int max, ArchiveLayout.Rect c) { ArchiveUi.label(g, font, heading.getString(), c.x() + ArchiveLayout.GAP, c.y() + 4); ArchiveUi.label(g, font, Component.translatable("rolecard.mission.admin.characters", length, max).getString(), c.right() - 82, c.y() + 4); }
    private void renderObjectives(GuiGraphics g) { ArchiveLayout.MissionEditor editor = ArchiveLayout.missionEditor(layout, objectives.size(), objectiveScroll); ArchiveUi.label(g, font, "目标（完成 / 内容 / 删除）", editor.toolbar().x(), editor.toolbar().y() + 6); for (int i = 0; i < editor.rows().size(); i++) { ArchiveLayout.ObjectiveRow row = editor.rows().get(i); if (!editor.list().contains(row.text())) continue; ArchiveUi.section(g, new ArchiveLayout.Rect(row.number().x(), row.number().y(), row.number().width(), row.number().height())); ArchiveUi.label(g, font, String.valueOf(i + 1), row.number().x() + 6, row.baseline()); } }
    private void renderRules(GuiGraphics g, ArchiveLayout.Rect c) { ArchiveUi.label(g, font, Component.translatable("rolecard.mission.rules").getString(), c.x() + ArchiveLayout.GAP, c.y() + 4); ArchiveUi.label(g, font, Component.translatable("rolecard.mission.notes").getString(), c.x() + ArchiveLayout.GAP, notes.getY() - 12); }
    /** CI 探针入口：真实控件路径仍经 Screen.mouseClicked/charTyped；此方法只供观测状态。 */
    public void ciShowPage(int value) { showPage(value); }
    public static void installCiPacketObserver(Consumer<AdminSaveMissionPacket> observer) { ciPacketObserver = observer; }
    public ArchiveLayout.Rect ciTabBounds(int index) { return layout.tab(index, 4); }
    public ArchiveLayout.Rect ciAddObjectiveBounds() { return ArchiveLayout.missionEditor(layout, objectives.size(), objectiveScroll).add(); }
    public ArchiveLayout.Rect ciPublishBounds() { return ArchiveLayout.playerActions(layout).submit(); }
    public ArchiveLayout.Rect ciObjectiveTextBounds(int index) { return ArchiveLayout.missionEditor(layout, objectives.size(), objectiveScroll).rows().get(index).text(); }
    public ArchiveLayout.Rect ciObjectiveToggleBounds(int index) { return ArchiveLayout.missionEditor(layout, objectives.size(), objectiveScroll).rows().get(index).toggle(); }
    public ArchiveLayout.Rect ciObjectiveRemoveBounds(int index) { return ArchiveLayout.missionEditor(layout, objectives.size(), objectiveScroll).rows().get(index).remove(); }
    public int ciObjectiveCount() { return objectives.size(); }
    public boolean ciObjectiveCompleted(int index) { return objectives.get(index).completed(); }
    public String ciObjectiveText(int index) { return objectives.get(index).text(); }
    public CompoundTag ciSnapshotForVerification() { commitObjectives(); return new MissionBoardSnapshot(MissionBoardSnapshot.DATA_VERSION, original.revision(), title.getValue(), summary.getValue(), List.copyOf(objectives), rules.getValue(), notes.getValue(), instanceName.getValue(), instanceCode.getValue(), difficulty.getValue(), players.getValue(), timeLimit.getValue(), status, 0L, "").toTag(); }
    public boolean ciLayoutWithinSafeArea() { return layout != null && layout.safe().contains(layout.panel()) && !layout.content().intersects(layout.footer()) && !layout.tabs().intersects(layout.content()); }
    @Override public boolean isPauseScreen() { return false; }
}
