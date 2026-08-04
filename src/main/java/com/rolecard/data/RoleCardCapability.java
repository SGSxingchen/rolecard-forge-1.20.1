package com.rolecard.data;

import com.rolecard.RoleCardMod;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;

public final class RoleCardCapability {
    public static final Capability<CharacterCard> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});
    public static final ResourceLocation ID = new ResourceLocation(RoleCardMod.MOD_ID, "card");

    public static final class Provider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
        private final CharacterCard card = new CharacterCard();
        private final LazyOptional<CharacterCard> holder = LazyOptional.of(() -> card);
        @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
            return capability == CAPABILITY ? holder.cast() : LazyOptional.empty();
        }
        @Override public CompoundTag serializeNBT() { return card.save(); }
        @Override public void deserializeNBT(CompoundTag tag) { card.load(tag); }
        public void invalidate() { holder.invalidate(); }
    }

    private RoleCardCapability() {}
}
