package com.rolecard.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/** 去除原版灰石外观的单行输入框；输入和无障碍仍由 EditBox 处理。 */
public final class ArchiveEditBox extends EditBox {
    public ArchiveEditBox(Font font, int x, int y, int width, Component message) {
        super(font, x, y, width, 20, message);
        setBordered(false);
        setTextColor(ArchiveUi.TEXT);
        setTextColorUneditable(ArchiveUi.MUTED);
    }
    @Override public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float tick) {
        int border = !isActive() ? ArchiveUi.BORDER_SUBTLE : isFocused() ? ArchiveUi.ACCENT : ArchiveUi.BORDER;
        g.fill(getX(), getY(), getX() + width, getY() + height, !isActive() ? 0xFF17212A : 0xFF0D1822);
        g.fill(getX(), getY(), getX() + width, getY() + 1, border);
        g.fill(getX(), getY() + height - 1, getX() + width, getY() + height, border);
        g.fill(getX(), getY(), getX() + 1, getY() + height, border);
        g.fill(getX() + width - 1, getY(), getX() + width, getY() + height, border);
        super.renderWidget(g, mouseX, mouseY, tick);
    }
}
