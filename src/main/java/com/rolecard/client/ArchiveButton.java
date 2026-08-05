package com.rolecard.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;

/** 保留原版 Button 的键盘、焦点和旁白行为，仅替换视觉皮肤。 */
public final class ArchiveButton extends Button {
    private final int tone;
    protected ArchiveButton(Button.Builder builder) { this(builder, ArchiveUi.ACCENT); }
    private ArchiveButton(Button.Builder builder, int tone) { super(builder); this.tone = tone; }
    public static ArchiveButton create(net.minecraft.network.chat.Component text, OnPress onPress, int x, int y, int width, int tone) {
        return Button.builder(text, onPress).bounds(x, y, width, 20).build(builder -> new ArchiveButton(builder, tone));
    }
    @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float tick) {
        int color = !active ? 0xFF26323A : isHoveredOrFocused() ? ArchiveUi.BUTTON_HOVER : ArchiveUi.BUTTON;
        if (isFocused()) color = ArchiveUi.BUTTON_FOCUS;
        g.fill(getX(), getY(), getX() + width, getY() + height, color);
        g.fill(getX(), getY(), getX() + width, getY() + 1, active ? tone : ArchiveUi.BORDER_SUBTLE);
        g.fill(getX(), getY() + height - 1, getX() + width, getY() + height, ArchiveUi.BORDER_SUBTLE);
        int textColor = active ? ArchiveUi.TEXT : ArchiveUi.MUTED;
        g.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, getMessage(), getX() + width / 2, getY() + 6, textColor);
    }
}
