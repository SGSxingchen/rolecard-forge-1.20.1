package com.rolecard.client;

/** 不依赖 Minecraft 的档案界面几何模型，供 Screen 与自动布局检查共同使用。 */
public final class ArchiveLayout {
    public record Rect(int x, int y, int width, int height) {
        public int right() { return x + width; }
        public int bottom() { return y + height; }
        public boolean contains(Rect child) { return child.x >= x && child.y >= y && child.right() <= right() && child.bottom() <= bottom(); }
        public boolean intersects(Rect other) { return x < other.right() && right() > other.x && y < other.bottom() && bottom() > other.y; }
    }

    public record Frame(Rect safe, Rect panel, Rect header, Rect tabs, Rect content, Rect feedback, Rect footer) {
        /** 三页审核档案与四页玩家档案共用同一条页签轨道。 */
        public Rect tab(int index) { return tab(index, 3); }
        public Rect tab(int index, int count) {
            int safeCount = Math.max(1, count);
            int safeIndex = Math.max(0, Math.min(safeCount - 1, index));
            int base = tabs.width / safeCount;
            int x = tabs.x + base * safeIndex;
            return new Rect(x, tabs.y, safeIndex == safeCount - 1 ? tabs.right() - x : base, tabs.height);
        }
    }
    public record PlayerActions(Rect close, Rect save, Rect submit) {}
    public record AdminActions(Rect close, Rect save, Rect reject, Rect approve, Rect unlock) {}

    public static Frame frame(int screenWidth, int screenHeight) {
        int inset = Math.max(8, Math.min(16, Math.min(screenWidth, screenHeight) / 12));
        Rect safe = new Rect(inset, inset, Math.max(1, screenWidth - inset * 2), Math.max(1, screenHeight - inset * 2));
        int panelWidth = Math.min(660, safe.width);
        int panelHeight = Math.min(430, safe.height);
        Rect panel = new Rect(safe.x + (safe.width - panelWidth) / 2, safe.y + (safe.height - panelHeight) / 2, panelWidth, panelHeight);
        int headerHeight = panelHeight < 270 ? 28 : 34;
        int tabHeight = panelHeight < 270 ? 20 : 24;
        int footerHeight = 24;
        Rect header = new Rect(panel.x + 2, panel.y + 2, panel.width - 4, headerHeight);
        Rect tabs = new Rect(panel.x + 8, header.bottom() + 4, panel.width - 16, tabHeight);
        Rect footer = new Rect(panel.x + 8, panel.bottom() - footerHeight - 8, panel.width - 16, footerHeight);
        Rect feedback = new Rect(panel.x + 8, footer.y - 17, panel.width - 16, 13);
        Rect content = new Rect(panel.x + 8, tabs.bottom() + 8, panel.width - 16, Math.max(1, feedback.y - (tabs.bottom() + 8) - 4));
        return new Frame(safe, panel, header, tabs, content, feedback, footer);
    }

    public static Rect inset(Rect source, int amount) { return new Rect(source.x + amount, source.y + amount, Math.max(1, source.width - amount * 2), Math.max(1, source.height - amount * 2)); }
    public static PlayerActions playerActions(Frame frame) {
        Rect footer = frame.footer(); Rect submit = new Rect(footer.right() - 96, footer.y(), 96, 20);
        return new PlayerActions(new Rect(footer.x(), footer.y(), 52, 20), new Rect(submit.x() - 84, footer.y(), 80, 20), submit);
    }
    public static AdminActions adminActions(Frame frame) {
        Rect footer = frame.footer(); int saveWidth = footer.width() >= 296 ? 72 : 52; Rect unlock = new Rect(footer.right() - 52, footer.y(), 52, 20); Rect approve = new Rect(unlock.x() - 56, footer.y(), 52, 20); Rect reject = new Rect(approve.x() - 56, footer.y(), 52, 20); Rect save = new Rect(reject.x() - 4 - saveWidth, footer.y(), saveWidth, 20);
        return new AdminActions(new Rect(footer.x(), footer.y(), 52, 20), save, reject, approve, unlock);
    }
    private ArchiveLayout() {}
}
