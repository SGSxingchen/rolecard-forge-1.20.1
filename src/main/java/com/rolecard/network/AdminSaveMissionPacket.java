package com.rolecard.network;

import com.rolecard.mission.MissionBoardSnapshot;
import com.rolecard.service.MissionBoardService;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** 管理界面的唯一 C2S 写包；服务器二次校验 OP、revision、字段类型和文本上限。 */
public record AdminSaveMissionPacket(long revision, CompoundTag snapshot) {
    static void encode(AdminSaveMissionPacket packet, FriendlyByteBuf buffer) { buffer.writeLong(packet.revision); buffer.writeNbt(packet.snapshot); }
    static AdminSaveMissionPacket decode(FriendlyByteBuf buffer) { long revision = buffer.readLong(); CompoundTag tag = buffer.readNbt(); return new AdminSaveMissionPacket(revision, tag); }
    static void handle(AdminSaveMissionPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get(); ServerPlayer admin = context.getSender();
        if (admin != null) context.enqueueWork(() -> {
            if (!admin.hasPermissions(2)) { deny(admin, "权限不足：没有任务管理权限。"); return; }
            if (!MissionBoardSnapshot.isValidClientTag(packet.snapshot)) { deny(admin, "保存失败：任务资料包含非法字段或超过长度限制。"); return; }
            MissionBoardService.MutationResult result = MissionBoardService.replace(admin.server, packet.revision, MissionBoardSnapshot.fromTag(packet.snapshot), admin.getGameProfile().getName());
            if (result.stale()) { deny(admin, "保存失败：公告已在其他位置更新，请确认最新版本后重试。"); return; }
            RoleCardNetwork.openAdminMission(admin, result.snapshot());
            if (!result.changed()) RoleCardNetwork.feedback(admin, "未发布：内容没有变化。", false);
        });
        context.setPacketHandled(true);
    }
    private static void deny(ServerPlayer admin, String text) { admin.sendSystemMessage(Component.literal(text)); RoleCardNetwork.feedback(admin, text, true); }
}
