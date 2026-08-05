package com.rolecard.client;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** 原版 EditBox 输入行为上的多行、裁剪和滚动显示层。 */
public final class MultiLineBiographyBox extends EditBox {
    private final Font textFont;
    private int firstLine;

    public MultiLineBiographyBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
        textFont = font;
        setBordered(false);
        setTextColor(ArchiveUi.TEXT);
        setTextColorUneditable(ArchiveUi.MUTED);
    }
    private List<FormattedCharSequence> lines() { return textFont.split(Component.literal(getValue()), Math.max(1, width - 14)); }
    private int visibleLines() { return Math.max(1, (height - 8) / 9); }
    private void clampScroll(List<FormattedCharSequence> lines) { firstLine = Math.max(0, Math.min(Math.max(0, lines.size() - visibleLines()), firstLine)); }
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        List<FormattedCharSequence> lines = lines();
        firstLine -= (int) Math.signum(delta);
        clampScroll(lines);
        return true;
    }
    @Override public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float tick) {
        int border = !active ? ArchiveUi.BORDER_SUBTLE : isFocused() ? ArchiveUi.ACCENT : ArchiveUi.BORDER;
        g.fill(getX(), getY(), getX() + width, getY() + height, !active ? 0xFF17212A : 0xFF0D1822);
        g.fill(getX(), getY(), getX() + width, getY() + 1, border);
        g.fill(getX(), getY() + height - 1, getX() + width, getY() + height, border);
        g.fill(getX(), getY(), getX() + 1, getY() + height, border);
        g.fill(getX() + width - 1, getY(), getX() + width, getY() + height, border);
        List<FormattedCharSequence> lines = lines(); clampScroll(lines);
        ArchiveUi.clip(g, new ArchiveLayout.Rect(getX() + 4, getY() + 3, width - 10, height - 6));
        if (lines.isEmpty() && !isFocused()) g.drawString(textFont, getMessage(), getX() + 5, getY() + 5, ArchiveUi.MUTED, false);
        for (int index = 0; index < visibleLines() && index + firstLine < lines.size(); index++)
            g.drawString(textFont, lines.get(index + firstLine), getX() + 5, getY() + 4 + index * 9, active ? ArchiveUi.TEXT : ArchiveUi.MUTED, false);
        ArchiveUi.noClip(g);
        if (lines.size() > visibleLines()) {
            int track = height - 8, thumb = Math.max(8, track * visibleLines() / lines.size());
            int offset = (track - thumb) * firstLine / Math.max(1, lines.size() - visibleLines());
            g.fill(getX() + width - 5, getY() + 4 + offset, getX() + width - 3, getY() + 4 + offset + thumb, ArchiveUi.MUTED);
        }
    }
}
