package com.rolecard.attribute;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public record AttributeRule(Supplier<Attribute> target, AttributeModifier.Operation operation, double coefficient, int cap, boolean enabled, UUID id) {
    public static AttributeRule of(String key, Supplier<Attribute> target, AttributeModifier.Operation operation, double coefficient, int cap) {
        return new AttributeRule(target, operation, coefficient, cap, true,
                UUID.nameUUIDFromBytes(("rolecard:" + key).getBytes(StandardCharsets.UTF_8)));
    }
}
