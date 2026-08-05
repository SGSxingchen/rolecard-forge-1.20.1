package com.rolecard.client;

import com.rolecard.data.ReviewStatus;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** 角色档案册的单一视觉 token 与原生 GuiGraphics 绘制辅助。 */
public final class ArchiveUi {
    public static final int GAP = 8;
    public static final int PANEL = 0xF20D1520, PANEL_INSET = 0xFF121E2B, SECTION = 0xFF172535;
    public static final int BORDER = 0xFF48677C, BORDER_SUBTLE = 0xFF273D50, TITLE = 0xFFF4E8C8;
    public static final int TEXT = 0xFFE3E7E1, MUTED = 0xFF9DAFBA, ACCENT = 0xFF65C6D8;
    public static final int SUCCESS = 0xFF79C996, WARNING = 0xFFF0BF64, ERROR = 0xFFE9857B;
    public static final int BUTTON = 0xFF20374B, BUTTON_HOVER = 0xFF2C5368, BUTTON_FOCUS = 0xFF376B81;

    public static void panel(GuiGraphics g, ArchiveLayout.Rect r) {
        g.fill(r.x(), r.y(), r.right(), r.bottom(), PANEL);
        g.fill(r.x(), r.y(), r.right(), r.y() + 1, BORDER);
        g.fill(r.x(), r.bottom() - 1, r.right(), r.bottom(), BORDER_SUBTLE);
        g.fill(r.x(), r.y(), r.x() + 1, r.bottom(), BORDER);
        g.fill(r.right() - 1, r.y(), r.right(), r.bottom(), BORDER_SUBTLE);
        g.fill(r.x() + 2, r.y() + 2, r.right() - 2, r.bottom() - 2, PANEL_INSET);
        g.fill(r.x(), r.y(), r.x() + 3, r.y() + 3, 0x00000000);
    }
    public static void section(GuiGraphics g, ArchiveLayout.Rect r) {
        g.fill(r.x(), r.y(), r.right(), r.bottom(), SECTION);
        g.fill(r.x(), r.y(), r.right(), r.y() + 1, BORDER_SUBTLE);
        g.fill(r.x(), r.bottom() - 1, r.right(), r.bottom(), BORDER_SUBTLE);
    }
    public static void header(GuiGraphics g, Font font, ArchiveLayout.Rect r, Component heading, Component right) {
        g.fill(r.x(), r.y(), r.right(), r.bottom(), 0xFF1D3447);
        g.fill(r.x(), r.bottom() - 1, r.right(), r.bottom(), BORDER);
        g.drawString(font, heading, r.x() + GAP, r.y() + (r.height() - 8) / 2, TITLE, false);
        int rightX = Math.max(r.x() + 100, r.right() - GAP - font.width(right));
        g.drawString(font, right, rightX, r.y() + (r.height() - 8) / 2, MUTED, false);
    }
    public static int statusColor(ReviewStatus status) { return switch (status) { case DRAFT -> MUTED; case PENDING -> WARNING; case APPROVED -> SUCCESS; case REJECTED -> ERROR; }; }
    public static void badge(GuiGraphics g, Font font, String text, int x, int y, int color) {
        int w = font.width(text) + 8;
        g.fill(x, y, x + w, y + 14, 0xFF101A23);
        g.fill(x, y, x + w, y + 1, color);
        g.drawString(font, text, x + 4, y + 3, color, false);
    }
    public static void label(GuiGraphics g, Font font, String text, int x, int y) { g.drawString(font, text, x, y, MUTED, false); }
    public static void clip(GuiGraphics g, ArchiveLayout.Rect r) { g.enableScissor(r.x(), r.y(), r.right(), r.bottom()); }
    public static void noClip(GuiGraphics g) { g.disableScissor(); }
    private ArchiveUi() {}
}
