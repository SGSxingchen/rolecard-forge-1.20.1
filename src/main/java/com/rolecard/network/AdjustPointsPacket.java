package com.rolecard.network;

import com.rolecard.data.RoleCardCapability;
import com.rolecard.data.StatType;
import com.rolecard.service.RoleCardService;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record AdjustPointsPacket(long revision, String stat, int delta) {
    static void encode(AdjustPointsPacket p, FriendlyByteBuf b) { b.writeLong(p.revision); b.writeUtf(p.stat, 24); b.writeInt(p.delta); }
    static AdjustPointsPacket decode(FriendlyByteBuf b) { return new AdjustPointsPacket(b.readLong(), b.readUtf(24), b.readInt()); }
    static void handle(AdjustPointsPacket p, Supplier<NetworkEvent.Context> s) { NetworkEvent.Context c=s.get(); ServerPlayer player=c.getSender(); if(player!=null)c.enqueueWork(()->player.getCapability(RoleCardCapability.CAPABILITY).ifPresent(card->{if(!RoleCardService.adjustPoint(player,card,p.revision,StatType.fromKey(p.stat),p.delta)) RoleCardService.stale(player);})); c.setPacketHandled(true); }
}
