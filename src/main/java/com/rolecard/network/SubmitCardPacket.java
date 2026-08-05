package com.rolecard.network;

import com.rolecard.data.RoleCardCapability;
import com.rolecard.service.RoleCardService;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record SubmitCardPacket(long revision) {
    static void encode(SubmitCardPacket p, FriendlyByteBuf b) { b.writeLong(p.revision); }
    static SubmitCardPacket decode(FriendlyByteBuf b) { return new SubmitCardPacket(b.readLong()); }
    static void handle(SubmitCardPacket p, Supplier<NetworkEvent.Context> s) { NetworkEvent.Context c=s.get(); ServerPlayer player=c.getSender(); if(player!=null)c.enqueueWork(()->player.getCapability(RoleCardCapability.CAPABILITY).ifPresent(card->{if(!RoleCardService.submit(player,card,p.revision)) RoleCardService.stale(player);})); c.setPacketHandled(true); }
}
