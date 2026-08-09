package com.rolecard.client;

import com.rolecard.mission.MissionBoardSnapshot;
import com.rolecard.mission.MissionObjective;
import com.rolecard.mission.MissionStatus;
import com.rolecard.network.AdminSaveMissionPacket;
import com.rolecard.network.RoleCardNetwork;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** 管理员任务公告编辑器；只发受限 DTO，发布权限和 revision 一律由服务端复核。 */
public final class AdminMissionScreen extends Screen {
    private final MissionBoardSnapshot original;
    private final List<MissionObjective> objectives = new ArrayList<>();
    private ArchiveLayout.Frame layout;
    private ArchiveButton[] tabs;
    private ArchiveButton close, publish, statusButton, previousObjective, nextObjective, toggleObjective, addObjective, removeObjective;
    private ArchiveEditBox title, instanceName, instanceCode, difficulty, players, timeLimit, objectiveText;
    private MultiLineBiographyBox summary, rules, notes;
    private MissionStatus status;
    private int page, basicScroll, selectedObjective, feedbackTicks;
    private String feedback = ""; private int feedbackColor = ArchiveUi.WARNING;

    public AdminMissionScreen(CompoundTag data) { super(Component.translatable("rolecard.mission.admin.title")); original = MissionBoardSnapshot.fromTag(data); objectives.addAll(original.objectives()); status = original.status(); }
    @Override protected void init() {
        layout = ArchiveLayout.frame(width, height); ArchiveLayout.Rect c = layout.content();
        title = field(0, 0, "rolecard.mission.field.title", original.title(), MissionBoardSnapshot.MAX_TITLE_LENGTH);
        instanceName = field(0, 0, "rolecard.mission.field.instance", original.instanceName(), MissionBoardSnapshot.MAX_INSTANCE_NAME_LENGTH);
        instanceCode = field(0, 0, "rolecard.mission.field.instance_code", original.instanceCode(), MissionBoardSnapshot.MAX_INSTANCE_CODE_LENGTH);
        difficulty = field(0, 0, "rolecard.mission.field.difficulty", original.difficulty(), MissionBoardSnapshot.MAX_DIFFICULTY_LENGTH);
        players = field(0, 0, "rolecard.mission.field.players", original.playerCountText(), MissionBoardSnapshot.MAX_PLAYER_COUNT_LENGTH);
        timeLimit = field(0, 0, "rolecard.mission.field.time_limit", original.timeLimitText(), MissionBoardSnapshot.MAX_TIME_LIMIT_LENGTH);
        objectiveText = field(c.x(), c.y() + 43, "rolecard.mission.objective_text", "", MissionBoardSnapshot.MAX_OBJECTIVE_LENGTH);
        summary = multiline(c.x(), c.y() + 16, c.width(), Math.max(30, c.height() - 16), "rolecard.mission.field.summary", original.summary(), MissionBoardSnapshot.MAX_SUMMARY_LENGTH);
        int half = Math.max(28, (c.height() - 32) / 2);
        rules = multiline(c.x(), c.y() + 16, c.width(), half, "rolecard.mission.field.rules", original.rules(), MissionBoardSnapshot.MAX_RULES_LENGTH);
        notes = multiline(c.x(), c.y() + 31 + half, c.width(), Math.max(28, c.height() - 31 - half), "rolecard.mission.field.notes", original.notes(), MissionBoardSnapshot.MAX_NOTES_LENGTH);
        addRenderableWidget(title); addRenderableWidget(instanceName); addRenderableWidget(instanceCode); addRenderableWidget(difficulty); addRenderableWidget(players); addRenderableWidget(timeLimit); addRenderableWidget(objectiveText); addRenderableWidget(summary); addRenderableWidget(rules); addRenderableWidget(notes);
        tabs = new ArchiveButton[4]; String[] keys = {"rolecard.mission.admin.basic", "rolecard.mission.summary", "rolecard.mission.objectives", "rolecard.mission.admin.rules_notes"};
        for (int i = 0; i < tabs.length; i++) { final int target = i; ArchiveLayout.Rect r = layout.tab(i, tabs.length); tabs[i] = ArchiveButton.create(Component.translatable(keys[i]), b -> showPage(target), r.x(), r.y(), r.width(), ArchiveUi.ACCENT); addRenderableWidget(tabs[i]); }
        ArchiveLayout.PlayerActions actions = ArchiveLayout.playerActions(layout);
        close = ArchiveButton.create(Component.translatable("rolecard.common.close"), b -> onClose(), actions.close().x(), actions.close().y(), actions.close().width(), ArchiveUi.MUTED);
        publish = ArchiveButton.create(Component.translatable("rolecard.mission.admin.publish"), b -> publish(), actions.submit().x(), actions.submit().y(), actions.submit().width(), ArchiveUi.ACCENT); addRenderableWidget(close); addRenderableWidget(publish);
        statusButton = ArchiveButton.create(Component.literal(""), b -> { status = MissionStatus.values()[(status.ordinal() + 1) % MissionStatus.values().length]; updateStatusButton(); }, 0, 0, 82, ArchiveUi.WARNING);
        previousObjective = button("‹", b -> selectObjective(selectedObjective - 1), 0, 0, 20, ArchiveUi.MUTED); nextObjective = button("›", b -> selectObjective(selectedObjective + 1), 0, 0, 20, ArchiveUi.MUTED);
        toggleObjective = button("rolecard.mission.admin.toggle", b -> toggleObjective(), 0, 0, 48, ArchiveUi.SUCCESS); addObjective = button("rolecard.mission.admin.add", b -> addObjective(), 0, 0, 36, ArchiveUi.ACCENT); removeObjective = button("rolecard.mission.admin.remove", b -> removeObjective(), 0, 0, 36, ArchiveUi.ERROR);
        addRenderableWidget(statusButton); addRenderableWidget(previousObjective); addRenderableWidget(nextObjective); addRenderableWidget(toggleObjective); addRenderableWidget(addObjective); addRenderableWidget(removeObjective);
        if (!objectives.isEmpty()) objectiveText.setValue(objectives.get(0).text());
        showPage(0);
    }
    private ArchiveEditBox field(int x, int y, String key, String value, int max) { ArchiveEditBox box = new ArchiveEditBox(font, x, y, 30, Component.translatable(key)); box.setValue(value); box.setMaxLength(max); return box; }
    private MultiLineBiographyBox multiline(int x, int y, int w, int h, String key, String value, int max) { MultiLineBiographyBox box = new MultiLineBiographyBox(font, x, y, w, h, Component.translatable(key)); box.setValue(value); box.setMaxLength(max); return box; }
    private ArchiveButton button(String key, net.minecraft.client.gui.components.Button.OnPress press, int x, int y, int w, int tone) { return ArchiveButton.create(key.length() == 1 ? Component.literal(key) : Component.translatable(key), press, x, y, w, tone); }
    private void showPage(int value) { page = value; basicScroll = 0; updateVisibility(); }
    private void updateVisibility() {
        boolean basic = page == 0, objective = page == 2;
        title.visible = basic; instanceName.visible = basic; instanceCode.visible = basic; difficulty.visible = basic; players.visible = basic; timeLimit.visible = basic; statusButton.visible = basic;
        summary.visible = page == 1; rules.visible = page == 3; notes.visible = page == 3; objectiveText.visible = objective;
        previousObjective.visible = objective; nextObjective.visible = objective; toggleObjective.visible = objective; addObjective.visible = objective; removeObjective.visible = objective;
        for (int i = 0; i < tabs.length; i++) tabs[i].active = i != page;
        positionBasic(); positionObjectives(); updateStatusButton();
    }
    private void positionBasic() { if (layout == null) return; ArchiveLayout.Rect c = layout.content(); ArchiveEditBox[] fields = {title, instanceName, instanceCode, difficulty, players, timeLimit}; String[] labels = {"rolecard.mission.field.title", "rolecard.mission.field.instance", "rolecard.mission.field.instance_code", "rolecard.mission.field.difficulty", "rolecard.mission.field.players", "rolecard.mission.field.time_limit"}; for (int i = 0; i < fields.length; i++) { int y = c.y() + 16 + i * 30 - basicScroll; fields[i].setX(c.x()); fields[i].setY(y); fields[i].setWidth(c.width()); } statusButton.setX(c.x()); statusButton.setY(c.y() + 16 + fields.length * 30 - basicScroll); }
    private void positionObjectives() { if (layout == null) return; ArchiveLayout.Rect c = layout.content(); int y = c.y() + 18; previousObjective.setX(c.x()); previousObjective.setY(y); nextObjective.setX(c.x() + 22); nextObjective.setY(y); toggleObjective.setX(c.x() + 46); toggleObjective.setY(y); addObjective.setX(c.right() - 76); addObjective.setY(y); removeObjective.setX(c.right() - 38); removeObjective.setY(y); objectiveText.setX(c.x()); objectiveText.setY(y + 25); objectiveText.setWidth(c.width()); }
    private void updateStatusButton() { statusButton.setMessage(Component.translatable(status.translationKey())); }
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) { if (page == 0 && layout.content().x() <= mouseX && mouseX < layout.content().right()) { basicScroll = Math.max(0, Math.min(104, basicScroll - (int) Math.signum(delta) * 16)); positionBasic(); return true; } return super.mouseScrolled(mouseX, mouseY, delta); }
    private void selectObjective(int target) { saveSelectedObjective(); if (objectives.isEmpty()) { selectedObjective = 0; objectiveText.setValue(""); return; } selectedObjective = Math.max(0, Math.min(objectives.size() - 1, target)); objectiveText.setValue(objectives.get(selectedObjective).text()); }
    private void saveSelectedObjective() { if (!objectives.isEmpty()) objectives.set(selectedObjective, new MissionObjective(objectives.get(selectedObjective).completed(), objectiveText.getValue())); }
    private void addObjective() { saveSelectedObjective(); if (objectives.size() >= MissionBoardSnapshot.MAX_OBJECTIVES) { notice(Component.translatable("rolecard.mission.admin.max_objectives").getString(), ArchiveUi.ERROR); return; } objectives.add(new MissionObjective(false, "")); selectObjective(objectives.size() - 1); }
    private void removeObjective() { if (objectives.isEmpty()) return; objectives.remove(selectedObjective); selectObjective(Math.min(selectedObjective, objectives.size() - 1)); }
    private void toggleObjective() { if (objectives.isEmpty()) return; saveSelectedObjective(); MissionObjective old = objectives.get(selectedObjective); objectives.set(selectedObjective, new MissionObjective(!old.completed(), old.text())); }
    private void publish() { saveSelectedObjective(); CompoundTag tag = new MissionBoardSnapshot(MissionBoardSnapshot.DATA_VERSION, original.revision(), title.getValue(), summary.getValue(), objectives, rules.getValue(), notes.getValue(), instanceName.getValue(), instanceCode.getValue(), difficulty.getValue(), players.getValue(), timeLimit.getValue(), status, 0L, "").toTag(); RoleCardNetwork.CHANNEL.sendToServer(new AdminSaveMissionPacket(original.revision(), tag)); notice(Component.translatable("rolecard.mission.admin.sending").getString(), ArchiveUi.SUCCESS); }
    public void receiveServerFeedback(String text, boolean error) { notice(text, error ? ArchiveUi.ERROR : ArchiveUi.SUCCESS); }
    private void notice(String text, int color) { feedback = text; feedbackColor = color; feedbackTicks = 100; }
    @Override public void tick() { super.tick(); if (feedbackTicks > 0) feedbackTicks--; }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float tick) {
        renderBackground(g); ArchiveUi.panel(g, layout.panel()); ArchiveUi.header(g, font, layout.header(), Component.translatable("rolecard.mission.admin.title"), Component.translatable(status.translationKey())); ArchiveUi.section(g, layout.content());
        ArchiveLayout.Rect c = layout.content(); ArchiveUi.clip(g, c);
        if (page == 0) renderBasic(g, c); else if (page == 1) renderTextPage(g, Component.translatable("rolecard.mission.summary"), summary.getValue().length(), MissionBoardSnapshot.MAX_SUMMARY_LENGTH, c); else if (page == 2) renderObjectives(g, c); else renderRules(g, c);
        ArchiveUi.noClip(g); if (feedbackTicks > 0) g.drawString(font, feedback, layout.feedback().x(), layout.feedback().y() + 2, feedbackColor, false); super.render(g, mouseX, mouseY, tick);
    }
    private void renderBasic(GuiGraphics g, ArchiveLayout.Rect c) { String[] keys = {"rolecard.mission.field.title", "rolecard.mission.field.instance", "rolecard.mission.field.instance_code", "rolecard.mission.field.difficulty", "rolecard.mission.field.players", "rolecard.mission.field.time_limit"}; for (int i = 0; i < keys.length; i++) ArchiveUi.label(g, font, Component.translatable(keys[i]).getString(), c.x(), c.y() + 5 + i * 30 - basicScroll); ArchiveUi.label(g, font, Component.translatable("rolecard.mission.field.status").getString(), c.x(), c.y() + 5 + keys.length * 30 - basicScroll); }
    private void renderTextPage(GuiGraphics g, Component heading, int length, int max, ArchiveLayout.Rect c) { ArchiveUi.label(g, font, heading.getString(), c.x(), c.y() + 4); ArchiveUi.label(g, font, Component.translatable("rolecard.mission.admin.characters", length, max).getString(), c.right() - 82, c.y() + 4); }
    private void renderObjectives(GuiGraphics g, ArchiveLayout.Rect c) { String label = objectives.isEmpty() ? Component.translatable("rolecard.mission.admin.no_objective").getString() : Component.translatable("rolecard.mission.admin.objective_index", selectedObjective + 1, objectives.size()).getString(); ArchiveUi.label(g, font, label, c.x(), c.y() + 5); if (!objectives.isEmpty()) ArchiveUi.label(g, font, objectives.get(selectedObjective).completed() ? Component.translatable("rolecard.mission.admin.completed").getString() : Component.translatable("rolecard.mission.admin.uncompleted").getString(), c.x() + 100, c.y() + 5); }
    private void renderRules(GuiGraphics g, ArchiveLayout.Rect c) { ArchiveUi.label(g, font, Component.translatable("rolecard.mission.rules").getString(), c.x(), c.y() + 4); ArchiveUi.label(g, font, Component.translatable("rolecard.mission.notes").getString(), c.x(), notes.getY() - 12); }
    /** CI 探针只验证初始化、切页及档案布局安全关系。 */
    public void ciShowPage(int value) { showPage(Math.max(0, Math.min(3, value))); }
    public boolean ciLayoutWithinSafeArea() { return layout != null && layout.safe().contains(layout.panel()) && !layout.content().intersects(layout.footer()) && !layout.tabs().intersects(layout.content()); }
    @Override public boolean isPauseScreen() { return false; }
}
