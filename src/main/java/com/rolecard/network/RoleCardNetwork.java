package com.rolecard.network;

import com.rolecard.RoleCardMod;
import com.rolecard.data.CharacterCard;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class RoleCardNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(RoleCardMod.MOD_ID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static int nextId;
    public static void register() {
        CHANNEL.messageBuilder(CardSyncPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CardSyncPacket::encode).decoder(CardSyncPacket::decode).consumerMainThread(CardSyncPacket::handle).add();
        CHANNEL.messageBuilder(EditIdentityPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(EditIdentityPacket::encode).decoder(EditIdentityPacket::decode).consumerMainThread(EditIdentityPacket::handle).add();
        CHANNEL.messageBuilder(PublicNamePacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PublicNamePacket::encode).decoder(PublicNamePacket::decode).consumerMainThread(PublicNamePacket::handle).add();
    }
    public static void sync(ServerPlayer player, CharacterCard card) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new CardSyncPacket(card.save())); }
    public static void syncPublicName(ServerPlayer player, CharacterCard card, boolean showOverhead) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new PublicNamePacket(player.getUUID(), showOverhead ? card.shownName(player.getGameProfile().getName()) : ""));
    }
    public static void syncPublicName(ServerPlayer recipient, Player target, CharacterCard card, boolean showOverhead) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> recipient), new PublicNamePacket(target.getUUID(), showOverhead ? card.shownName(target.getGameProfile().getName()) : ""));
    }
    private RoleCardNetwork() {}
}
