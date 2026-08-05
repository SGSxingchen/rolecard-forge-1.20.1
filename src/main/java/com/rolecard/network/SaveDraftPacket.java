package com.rolecard.network;

import com.rolecard.data.CharacterCard;
import com.rolecard.data.RoleCardCapability;
import com.rolecard.service.RoleCardService;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record SaveDraftPacket(long revision, String name, int age, String gender, String biography) {
    static void encode(SaveDraftPacket p, FriendlyByteBuf b) { b.writeLong(p.revision); b.writeUtf(p.name, CharacterCard.MAX_TEXT_UTF8_BYTES); b.writeInt(p.age); b.writeUtf(p.gender, CharacterCard.MAX_TEXT_UTF8_BYTES); b.writeUtf(p.biography, CharacterCard.MAX_BIOGRAPHY_UTF8_BYTES); }
    static SaveDraftPacket decode(FriendlyByteBuf b) { return new SaveDraftPacket(b.readLong(), b.readUtf(CharacterCard.MAX_TEXT_UTF8_BYTES), b.readInt(), b.readUtf(CharacterCard.MAX_TEXT_UTF8_BYTES), b.readUtf(CharacterCard.MAX_BIOGRAPHY_UTF8_BYTES)); }
    static void handle(SaveDraftPacket p, Supplier<NetworkEvent.Context> s) { NetworkEvent.Context c=s.get(); ServerPlayer player=c.getSender(); if(player!=null)c.enqueueWork(()->player.getCapability(RoleCardCapability.CAPABILITY).ifPresent(card->{if(!RoleCardService.saveDraft(player,card,p.revision,p.name,p.age,p.gender,p.biography)){RoleCardService.stale(player);}})); c.setPacketHandled(true); }
}
