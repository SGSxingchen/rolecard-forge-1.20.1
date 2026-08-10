package com.rolecard.client;

import java.util.ArrayList;
import java.util.List;

/** 不依赖 Minecraft 的档案界面几何模型；Screen、探针和人工审阅 JSON 共用它。 */
public final class ArchiveLayout {
    public static final int CONTROL_HEIGHT = 20;
    public static final int GAP = 8;

    public record Rect(int x, int y, int width, int height) {
        public int right() { return x + width; }
        public int bottom() { return y + height; }
        public int centerY() { return y + height / 2; }
        public boolean contains(Rect child) { return child.x >= x && child.y >= y && child.right() <= right() && child.bottom() <= bottom(); }
        public boolean intersects(Rect other) { return x < other.right() && right() > other.x && y < other.bottom() && bottom() > other.y; }
    }

    public record Frame(Rect safe, Rect panel, Rect header, Rect tabs, Rect content, Rect feedback, Rect footer) {
        /** 三页审核档案与四页玩家／任务档案共用同一条页签轨道。 */
        public Rect tab(int index) { return tab(index, 3); }
        public Rect tab(int index, int count) {
            int safeCount = Math.max(1, count), safeIndex = Math.max(0, Math.min(safeCount - 1, index));
            int base = tabs.width / safeCount, x = tabs.x + base * safeIndex;
            return new Rect(x, tabs.y, safeIndex == safeCount - 1 ? tabs.right() - x : base, tabs.height);
        }
    }
    public record PlayerActions(Rect close, Rect save, Rect submit) {}
    public record AdminActions(Rect close, Rect save, Rect reject, Rect approve, Rect unlock) {}
    /** 单行标签—字段组：baseline 是标签与字段文本共同使用的 9px 字体基线。 */
    public record FieldRow(Rect label, Rect field, int baseline) {}
    public record IdentityForm(Rect area, FieldRow name, FieldRow age, FieldRow gender, boolean compact) {}
    public record MissionEditor(Rect area, Rect toolbar, Rect add, Rect list, List<ObjectiveRow> rows) {}
    public record ObjectiveRow(Rect number, Rect toggle, Rect text, Rect remove, int baseline) {}

    public static Frame frame(int screenWidth, int screenHeight) {
        int inset = Math.max(8, Math.min(16, Math.min(screenWidth, screenHeight) / 12));
        Rect safe = new Rect(inset, inset, Math.max(1, screenWidth - inset * 2), Math.max(1, screenHeight - inset * 2));
        int panelWidth = Math.min(660, safe.width), panelHeight = Math.min(430, safe.height);
        Rect panel = new Rect(safe.x + (safe.width - panelWidth) / 2, safe.y + (safe.height - panelHeight) / 2, panelWidth, panelHeight);
        int headerHeight = panelHeight < 270 ? 28 : 34, tabHeight = panelHeight < 270 ? CONTROL_HEIGHT : 24;
        Rect header = new Rect(panel.x + 2, panel.y + 2, panel.width - 4, headerHeight);
        Rect tabs = new Rect(panel.x + GAP, header.bottom() + 4, panel.width - GAP * 2, tabHeight);
        Rect footer = new Rect(panel.x + GAP, panel.bottom() - CONTROL_HEIGHT - GAP, panel.width - GAP * 2, CONTROL_HEIGHT);
        Rect feedback = new Rect(panel.x + GAP, footer.y - 17, panel.width - GAP * 2, 13);
        Rect content = new Rect(panel.x + GAP, tabs.bottom() + GAP, panel.width - GAP * 2, Math.max(1, feedback.y - (tabs.bottom() + GAP) - 4));
        return new Frame(safe, panel, header, tabs, content, feedback, footer);
    }

    /** 玩家和审核身份页的唯一表单网格；窄屏自动转单列而不缩字号。 */
    public static IdentityForm identityForm(Frame frame) {
        Rect area = inset(frame.content(), GAP);
        int labelW = Math.min(52, Math.max(38, area.width() / 5));
        FieldRow name = row(area.x(), area.y() + 18, labelW, area.width());
        int groupGap = 6, groupW = (area.width() - groupGap) / 2;
        boolean compact = groupW < 112;
        FieldRow age = row(area.x(), name.field().bottom() + GAP, labelW, compact ? area.width() : groupW);
        FieldRow gender = compact
                ? row(area.x(), age.field().bottom() + GAP, labelW, area.width())
                : row(area.x() + groupW + groupGap, age.field().y(), labelW, area.width() - groupW - groupGap);
        return new IdentityForm(area, name, age, gender, compact);
    }
    private static FieldRow row(int x, int y, int labelWidth, int fullWidth) {
        Rect label = new Rect(x, y, labelWidth, CONTROL_HEIGHT);
        Rect field = new Rect(x + labelWidth + 4, y, Math.max(30, fullWidth - labelWidth - 4), CONTROL_HEIGHT);
        return new FieldRow(label, field, y + 6);
    }

    /** 任务目标稳定行模型。调用者仅切换 visible 和位置，绝不为新增目标重建 Screen。 */
    public static MissionEditor missionEditor(Frame frame, int objectiveCount, int scroll) {
        Rect area = inset(frame.content(), GAP);
        Rect toolbar = new Rect(area.x(), area.y(), area.width(), CONTROL_HEIGHT);
        Rect add = new Rect(Math.max(area.x(), area.right() - 56), toolbar.y(), 56, CONTROL_HEIGHT);
        Rect list = new Rect(area.x(), toolbar.bottom() + 4, area.width(), Math.max(1, area.bottom() - toolbar.bottom() - 4));
        List<ObjectiveRow> rows = new ArrayList<>();
        int y = list.y() - Math.max(0, scroll);
        for (int index = 0; index < Math.max(0, objectiveCount); index++) {
            Rect number = new Rect(list.x(), y, 20, CONTROL_HEIGHT);
            Rect toggle = new Rect(number.right() + 4, y, 20, CONTROL_HEIGHT);
            Rect remove = new Rect(list.right() - 20, y, 20, CONTROL_HEIGHT);
            Rect text = new Rect(toggle.right() + 4, y, Math.max(30, remove.x() - toggle.right() - 8), CONTROL_HEIGHT);
            rows.add(new ObjectiveRow(number, toggle, text, remove, y + 6));
            y += CONTROL_HEIGHT + 4;
        }
        return new MissionEditor(area, toolbar, add, list, List.copyOf(rows));
    }
    public static int missionObjectiveMaxScroll(Frame frame, int objectiveCount) {
        MissionEditor editor = missionEditor(frame, 0, 0);
        return Math.max(0, objectiveCount * (CONTROL_HEIGHT + 4) - 4 - editor.list().height());
    }

    public static Rect inset(Rect source, int amount) { return new Rect(source.x + amount, source.y + amount, Math.max(1, source.width - amount * 2), Math.max(1, source.height - amount * 2)); }
    public static PlayerActions playerActions(Frame frame) {
        Rect footer = frame.footer(), submit = new Rect(footer.right() - 96, footer.y(), 96, CONTROL_HEIGHT);
        return new PlayerActions(new Rect(footer.x(), footer.y(), 52, CONTROL_HEIGHT), new Rect(submit.x() - 84, footer.y(), 80, CONTROL_HEIGHT), submit);
    }
    public static AdminActions adminActions(Frame frame) {
        Rect footer = frame.footer(); int saveWidth = footer.width() >= 296 ? 72 : 52;
        Rect unlock = new Rect(footer.right() - 52, footer.y(), 52, CONTROL_HEIGHT), approve = new Rect(unlock.x() - 56, footer.y(), 52, CONTROL_HEIGHT), reject = new Rect(approve.x() - 56, footer.y(), 52, CONTROL_HEIGHT), save = new Rect(reject.x() - 4 - saveWidth, footer.y(), saveWidth, CONTROL_HEIGHT);
        return new AdminActions(new Rect(footer.x(), footer.y(), 52, CONTROL_HEIGHT), save, reject, approve, unlock);
    }
    private ArchiveLayout() {}
}
