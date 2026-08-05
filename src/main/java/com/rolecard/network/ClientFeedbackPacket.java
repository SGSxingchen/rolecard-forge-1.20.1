package com.rolecard.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** 仅用于把服务端拒绝原因显示回当前档案面板，不携带任何权威数据。 */
public record ClientFeedbackPacket(String message, boolean error) {
    static void encode(ClientFeedbackPacket packet, FriendlyByteBuf buffer) { buffer.writeUtf(packet.message, 256); buffer.writeBoolean(packet.error); }
    static ClientFeedbackPacket decode(FriendlyByteBuf buffer) { return new ClientFeedbackPacket(buffer.readUtf(256), buffer.readBoolean()); }
    static void handle(ClientFeedbackPacket packet, Supplier<NetworkEvent.Context> supplier) { supplier.get().enqueueWork(() -> ClientHooks.acceptFeedback(packet.message, packet.error)); supplier.get().setPacketHandled(true); }
}
