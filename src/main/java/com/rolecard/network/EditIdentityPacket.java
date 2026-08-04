package com.rolecard.network;

import com.rolecard.RoleCardEvents;
import com.rolecard.data.CharacterCard;
import com.rolecard.data.RoleCardCapability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record EditIdentityPacket(String name, int age, String gender) {
    static void encode(EditIdentityPacket packet, FriendlyByteBuf buffer) { buffer.writeUtf(packet.name, CharacterCard.MAX_TEXT_UTF8_BYTES); buffer.writeInt(packet.age); buffer.writeUtf(packet.gender, CharacterCard.MAX_TEXT_UTF8_BYTES); }
    static EditIdentityPacket decode(FriendlyByteBuf buffer) { return new EditIdentityPacket(buffer.readUtf(CharacterCard.MAX_TEXT_UTF8_BYTES), buffer.readInt(), buffer.readUtf(CharacterCard.MAX_TEXT_UTF8_BYTES)); }
    static void handle(EditIdentityPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();
        if (player != null) context.enqueueWork(() -> player.getCapability(RoleCardCapability.CAPABILITY).ifPresent(card -> {
            if (card.setIdentity(packet.name, packet.age, packet.gender)) {
                RoleCardEvents.syncAndApply(player);
                player.sendSystemMessage(Component.literal("角色身份已保存。"));
            } else player.sendSystemMessage(Component.literal("身份信息无效：名称和性别最多 32 字，年龄为 0-999。"));
        }));
        context.setPacketHandled(true);
    }
}
