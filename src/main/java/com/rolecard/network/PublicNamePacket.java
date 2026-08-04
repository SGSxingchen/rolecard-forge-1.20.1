package com.rolecard.network;

import java.util.UUID;
import java.util.function.Supplier;
import com.rolecard.data.CharacterCard;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record PublicNamePacket(UUID playerId, String name) {
    static void encode(PublicNamePacket packet, FriendlyByteBuf buffer) { buffer.writeUUID(packet.playerId); buffer.writeUtf(packet.name, CharacterCard.MAX_TEXT_UTF8_BYTES); }
    static PublicNamePacket decode(FriendlyByteBuf buffer) { return new PublicNamePacket(buffer.readUUID(), buffer.readUtf(CharacterCard.MAX_TEXT_UTF8_BYTES)); }
    static void handle(PublicNamePacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get(); context.enqueueWork(() -> ClientHooks.acceptPublicName(packet.playerId, packet.name)); context.setPacketHandled(true);
    }
}
