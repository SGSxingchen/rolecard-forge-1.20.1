package com.rolecard.network;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** Common 包不引用客户端 Screen；客户端安装回调后才会显示管理界面。 */
public record OpenAdminCardPacket(UUID target, String originalName, CompoundTag card) {
    static void encode(OpenAdminCardPacket p, FriendlyByteBuf b) { b.writeUUID(p.target); b.writeUtf(p.originalName, 64); b.writeNbt(p.card); }
    static OpenAdminCardPacket decode(FriendlyByteBuf b) { return new OpenAdminCardPacket(b.readUUID(), b.readUtf(64), b.readNbt()); }
    static void handle(OpenAdminCardPacket p, Supplier<NetworkEvent.Context> s) { NetworkEvent.Context c=s.get(); c.enqueueWork(()->ClientHooks.acceptAdmin(p.target,p.originalName,p.card)); c.setPacketHandled(true); }
}
