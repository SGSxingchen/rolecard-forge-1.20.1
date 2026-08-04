package com.rolecard.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record CardSyncPacket(CompoundTag data) {
    static void encode(CardSyncPacket packet, FriendlyByteBuf buffer) { buffer.writeNbt(packet.data); }
    static CardSyncPacket decode(FriendlyByteBuf buffer) { return new CardSyncPacket(buffer.readNbt()); }
    static void handle(CardSyncPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ClientHooks.acceptCard(packet.data));
        context.setPacketHandled(true);
    }
}
