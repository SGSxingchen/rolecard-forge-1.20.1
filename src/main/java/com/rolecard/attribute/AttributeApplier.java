package com.rolecard.attribute;

import com.rolecard.data.CharacterCard;
import com.rolecard.data.StatType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public final class AttributeApplier {
    public static void apply(ServerPlayer player, CharacterCard card) {
        for (StatType type : StatType.values()) {
            for (AttributeRule rule : AttributeMappings.rules(type)) {
                AttributeInstance instance = player.getAttribute(rule.target().get());
                if (instance == null) continue;
                instance.removeModifier(rule.id());
                if (rule.enabled()) instance.addPermanentModifier(new AttributeModifier(rule.id(), "rolecard_" + type.key(), AttributeMappings.amount(card, type, rule), rule.operation()));
            }
        }
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }
    private AttributeApplier() {}
}
