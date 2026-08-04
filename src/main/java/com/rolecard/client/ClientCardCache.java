package com.rolecard.client;

import com.rolecard.data.CharacterCard;
import net.minecraft.nbt.CompoundTag;

public final class ClientCardCache {
    private static CharacterCard card = new CharacterCard();
    public static CharacterCard card() { return card; }
    public static void update(CompoundTag data) { card = new CharacterCard(); if (data != null) card.load(data); }
    private ClientCardCache() {}
}
