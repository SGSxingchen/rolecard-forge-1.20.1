package com.rolecard.client;

import com.rolecard.attribute.AttributeMappings;
import com.rolecard.attribute.AttributeRule;
import com.rolecard.data.CharacterCard;
import com.rolecard.data.StatType;
import com.rolecard.network.EditIdentityPacket;
import com.rolecard.network.RoleCardNetwork;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;

public final class RoleCardScreen extends Screen {
    private EditBox nameBox;
    private EditBox ageBox;
    private EditBox genderBox;
    private String feedback = "";
    private int left;
    private int top;

    public RoleCardScreen() { super(Component.literal("角色档案")); }

    @Override protected void init() {
        int cardWidth = Math.max(220, Math.min(390, width - 20)); left = (width - cardWidth) / 2; top = Math.max(7, (height - 226) / 2);
        CharacterCard card = ClientCardCache.card();
        nameBox = new EditBox(font, left + 80, top + 42, cardWidth - 92, 18, Component.literal("角色名称")); nameBox.setValue(card.roleName()); nameBox.setMaxLength(CharacterCard.MAX_TEXT_LENGTH);
        ageBox = new EditBox(font, left + 52, top + 65, 50, 18, Component.literal("年龄")); ageBox.setValue(String.valueOf(card.age())); ageBox.setMaxLength(3);
        genderBox = new EditBox(font, left + 150, top + 65, cardWidth - 162, 18, Component.literal("性别")); genderBox.setValue(card.gender()); genderBox.setMaxLength(CharacterCard.MAX_TEXT_LENGTH);
        addRenderableWidget(nameBox); addRenderableWidget(ageBox); addRenderableWidget(genderBox);
        addRenderableWidget(Button.builder(Component.literal("确认保存"), button -> save()).bounds(left + cardWidth - 100, top + 196, 88, 20).build());
        addRenderableWidget(Button.builder(Component.literal("关闭"), button -> onClose()).bounds(left + 12, top + 196, 60, 20).build());
    }

    private void save() {
        try {
            int age = Integer.parseInt(ageBox.getValue());
            RoleCardNetwork.CHANNEL.sendToServer(new EditIdentityPacket(nameBox.getValue(), age, genderBox.getValue()));
            feedback = "已发送，服务器将校验并同步。";
        } catch (NumberFormatException exception) { feedback = "年龄必须是 0 到 999 的整数。"; }
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics); int cardWidth = Math.max(220, Math.min(390, width - 20)); left = (width - cardWidth) / 2; top = Math.max(7, (height - 226) / 2);
        graphics.fill(left, top, left + cardWidth, top + 226, 0xEE18202C);
        graphics.fill(left + 2, top + 2, left + cardWidth - 2, top + 30, 0xFF345170);
        graphics.drawCenteredString(font, title, left + cardWidth / 2, top + 11, 0xFFF5F1D8);
        graphics.drawString(font, "身份信息", left + 12, top + 35, 0xFFAED4E6, false);
        graphics.drawString(font, "名称", left + 14, top + 47, 0xFFE7E4D5, false);
        graphics.drawString(font, "年龄", left + 14, top + 70, 0xFFE7E4D5, false);
        graphics.drawString(font, "性别", left + 112, top + 70, 0xFFE7E4D5, false);
        graphics.fill(left + 12, top + 91, left + cardWidth - 12, top + 92, 0xFF496176);
        graphics.drawString(font, "六维属性（由管理员调整）", left + 12, top + 99, 0xFFAED4E6, false);
        CharacterCard card = ClientCardCache.card();
        int row = top + 112;
        for (int index = 0; index < StatType.values().length; index++) {
            StatType stat = StatType.values()[index]; int column = index % 2; int y = row + (index / 2) * 30; int x = left + 14 + column * (cardWidth / 2);
            graphics.fill(x, y, x + cardWidth / 2 - 20, y + 22, 0xFF223043);
            graphics.drawString(font, stat.displayName(), x + 7, y + 7, 0xFFF0EBD5, false);
            graphics.drawString(font, String.valueOf(card.stat(stat)), x + cardWidth / 2 - 48, y + 7, 0xFF9FE3BE, false);
            if (mouseX >= x && mouseX <= x + cardWidth / 2 - 20 && mouseY >= y && mouseY <= y + 22) graphics.renderTooltip(font, Component.literal(effectText(card, stat)), mouseX, mouseY);
        }
        graphics.drawString(font, "悬停属性可查看实际增益；不修改游戏账号真名。", left + 12, top + 188, 0xFFA8B7C5, false);
        if (!feedback.isEmpty()) graphics.drawString(font, feedback, left + 80, top + 202, 0xFFFFD27D, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private String effectText(CharacterCard card, StatType stat) {
        List<String> parts = new ArrayList<>();
        for (AttributeRule rule : AttributeMappings.rules(stat)) {
            Attribute attribute = rule.target().get();
            String operation = rule.operation().name().equals("ADDITION") ? "+" : "×";
            parts.add(attribute.getDescriptionId().replace("attribute.name.generic.", "") + operation + String.format("%.2f", AttributeMappings.amount(card, stat, rule)));
        }
        return stat.displayName() + "：" + String.join("，", parts);
    }

    @Override public boolean isPauseScreen() { return false; }
}
