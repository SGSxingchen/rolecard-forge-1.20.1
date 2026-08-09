package com.rolecard.network;

import com.rolecard.RoleCardMod;
import com.rolecard.data.CharacterCard;
import com.rolecard.mission.MissionBoardSnapshot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class RoleCardNetwork {
    private static final String PROTOCOL = "4";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(RoleCardMod.MOD_ID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
    private static int nextId;
    public static void register() {
        CHANNEL.messageBuilder(CardSyncPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT).encoder(CardSyncPacket::encode).decoder(CardSyncPacket::decode).consumerMainThread(CardSyncPacket::handle).add();
        CHANNEL.messageBuilder(PublicNamePacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT).encoder(PublicNamePacket::encode).decoder(PublicNamePacket::decode).consumerMainThread(PublicNamePacket::handle).add();
        CHANNEL.messageBuilder(SaveDraftPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER).encoder(SaveDraftPacket::encode).decoder(SaveDraftPacket::decode).consumerMainThread(SaveDraftPacket::handle).add();
        CHANNEL.messageBuilder(AdjustPointsPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER).encoder(AdjustPointsPacket::encode).decoder(AdjustPointsPacket::decode).consumerMainThread(AdjustPointsPacket::handle).add();
        CHANNEL.messageBuilder(SubmitCardPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER).encoder(SubmitCardPacket::encode).decoder(SubmitCardPacket::decode).consumerMainThread(SubmitCardPacket::handle).add();
        CHANNEL.messageBuilder(OpenAdminCardPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT).encoder(OpenAdminCardPacket::encode).decoder(OpenAdminCardPacket::decode).consumerMainThread(OpenAdminCardPacket::handle).add();
        CHANNEL.messageBuilder(ClientFeedbackPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT).encoder(ClientFeedbackPacket::encode).decoder(ClientFeedbackPacket::decode).consumerMainThread(ClientFeedbackPacket::handle).add();
        CHANNEL.messageBuilder(AdminReviewActionPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER).encoder(AdminReviewActionPacket::encode).decoder(AdminReviewActionPacket::decode).consumerMainThread(AdminReviewActionPacket::handle).add();
        CHANNEL.messageBuilder(AdminSaveCardPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER).encoder(AdminSaveCardPacket::encode).decoder(AdminSaveCardPacket::decode).consumerMainThread(AdminSaveCardPacket::handle).add();
        // 仅追加 discriminator，v1.1.x 包的编号和字段语义完全不变。
        CHANNEL.messageBuilder(MissionSyncPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT).encoder(MissionSyncPacket::encode).decoder(MissionSyncPacket::decode).consumerMainThread(MissionSyncPacket::handle).add();
        CHANNEL.messageBuilder(OpenMissionPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT).encoder(OpenMissionPacket::encode).decoder(OpenMissionPacket::decode).consumerMainThread(OpenMissionPacket::handle).add();
        CHANNEL.messageBuilder(OpenAdminMissionPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT).encoder(OpenAdminMissionPacket::encode).decoder(OpenAdminMissionPacket::decode).consumerMainThread(OpenAdminMissionPacket::handle).add();
        CHANNEL.messageBuilder(AdminSaveMissionPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER).encoder(AdminSaveMissionPacket::encode).decoder(AdminSaveMissionPacket::decode).consumerMainThread(AdminSaveMissionPacket::handle).add();
    }
    public static void sync(ServerPlayer player, CharacterCard card) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new CardSyncPacket(card.save())); }
    public static void openAdmin(ServerPlayer admin, ServerPlayer target, CharacterCard card) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> admin), new OpenAdminCardPacket(target.getUUID(), target.getGameProfile().getName(), card.save())); }
    public static void feedback(ServerPlayer player, String message, boolean error) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ClientFeedbackPacket(message, error)); }
    public static void syncPublicName(ServerPlayer player, CharacterCard card, boolean showOverhead) { CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new PublicNamePacket(player.getUUID(), showOverhead ? card.shownName(player.getGameProfile().getName()) : "")); }
    public static void syncPublicName(ServerPlayer recipient, Player target, CharacterCard card, boolean showOverhead) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> recipient), new PublicNamePacket(target.getUUID(), showOverhead ? card.shownName(target.getGameProfile().getName()) : "")); }
    public static void syncMission(ServerPlayer player, MissionBoardSnapshot snapshot) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new MissionSyncPacket(snapshot.toTag())); }
    public static void openMission(ServerPlayer player) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenMissionPacket()); }
    public static void openAdminMission(ServerPlayer player, MissionBoardSnapshot snapshot) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenAdminMissionPacket(snapshot.toTag())); }
    private RoleCardNetwork() {}
}
