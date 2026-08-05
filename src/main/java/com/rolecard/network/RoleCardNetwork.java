package com.rolecard.network;

import com.rolecard.RoleCardMod;
import com.rolecard.data.CharacterCard;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class RoleCardNetwork {
    private static final String PROTOCOL = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(RoleCardMod.MOD_ID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static int nextId;
    public static void register() {
        CHANNEL.messageBuilder(CardSyncPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT).encoder(CardSyncPacket::encode).decoder(CardSyncPacket::decode).consumerMainThread(CardSyncPacket::handle).add();
        CHANNEL.messageBuilder(PublicNamePacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT).encoder(PublicNamePacket::encode).decoder(PublicNamePacket::decode).consumerMainThread(PublicNamePacket::handle).add();
        CHANNEL.messageBuilder(SaveDraftPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER).encoder(SaveDraftPacket::encode).decoder(SaveDraftPacket::decode).consumerMainThread(SaveDraftPacket::handle).add();
        CHANNEL.messageBuilder(AdjustPointsPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER).encoder(AdjustPointsPacket::encode).decoder(AdjustPointsPacket::decode).consumerMainThread(AdjustPointsPacket::handle).add();
        CHANNEL.messageBuilder(SubmitCardPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER).encoder(SubmitCardPacket::encode).decoder(SubmitCardPacket::decode).consumerMainThread(SubmitCardPacket::handle).add();
        CHANNEL.messageBuilder(OpenAdminCardPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT).encoder(OpenAdminCardPacket::encode).decoder(OpenAdminCardPacket::decode).consumerMainThread(OpenAdminCardPacket::handle).add();
        CHANNEL.messageBuilder(AdminReviewActionPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER).encoder(AdminReviewActionPacket::encode).decoder(AdminReviewActionPacket::decode).consumerMainThread(AdminReviewActionPacket::handle).add();
        CHANNEL.messageBuilder(AdminSaveCardPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER).encoder(AdminSaveCardPacket::encode).decoder(AdminSaveCardPacket::decode).consumerMainThread(AdminSaveCardPacket::handle).add();
    }
    public static void sync(ServerPlayer player, CharacterCard card) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new CardSyncPacket(card.save())); }
    public static void openAdmin(ServerPlayer admin, ServerPlayer target, CharacterCard card) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> admin), new OpenAdminCardPacket(target.getUUID(), target.getGameProfile().getName(), card.save())); }
    public static void syncPublicName(ServerPlayer player, CharacterCard card, boolean showOverhead) { CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new PublicNamePacket(player.getUUID(), showOverhead ? card.shownName(player.getGameProfile().getName()) : "")); }
    public static void syncPublicName(ServerPlayer recipient, Player target, CharacterCard card, boolean showOverhead) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> recipient), new PublicNamePacket(target.getUUID(), showOverhead ? card.shownName(target.getGameProfile().getName()) : "")); }
    private RoleCardNetwork() {}
}
