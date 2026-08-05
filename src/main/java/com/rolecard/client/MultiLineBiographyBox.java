package com.rolecard.client;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * 基于原版 EditBox 的轻量多行显示层。输入、选择、剪贴板和焦点仍使用原版控件；
 * 本类只按可视宽度换行并用滚轮浏览，避免引入外部 UI 库。
 */
public final class MultiLineBiographyBox extends EditBox {
    private final Font textFont;
    private final int boxWidth;
    private final int boxHeight;
    private int firstLine;

    public MultiLineBiographyBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
        this.textFont = font;
        this.boxWidth = width;
        this.boxHeight = height;
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        List<FormattedCharSequence> lines = textFont.split(Component.literal(getValue()), Math.max(1, boxWidth - 8));
        int visible = Math.max(1, (boxHeight - 6) / 9);
        firstLine = Math.max(0, Math.min(Math.max(0, lines.size() - visible), firstLine - (int)Math.signum(delta)));
        return true;
    }

    @Override public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(getX(), getY(), getX() + boxWidth, getY() + boxHeight, 0xFF101720);
        graphics.fill(getX(), getY(), getX() + boxWidth, getY() + 1, isFocused() ? 0xFF77BEEA : 0xFF496176);
        List<FormattedCharSequence> lines = textFont.split(Component.literal(getValue()), Math.max(1, boxWidth - 8));
        int visible = Math.max(1, (boxHeight - 6) / 9);
        for (int index = 0; index < visible && index + firstLine < lines.size(); index++) {
            graphics.drawString(textFont, lines.get(index + firstLine), getX() + 4, getY() + 3 + index * 9, 0xFFE7E4D5, false);
        }
        if (lines.isEmpty()) graphics.drawString(textFont, "可输入换行；鼠标滚轮滚动。", getX() + 4, getY() + 4, 0xFF8A98A7, false);
    }
}
