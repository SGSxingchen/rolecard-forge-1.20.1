package com.rolecard.network;

import com.rolecard.mission.MissionBoardSnapshot;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** 管理员命令的 S2C 打开请求；权限在服务端命令入口验证。 */
public record OpenAdminMissionPacket(CompoundTag snapshot) {
    static void encode(OpenAdminMissionPacket packet, FriendlyByteBuf buffer) { buffer.writeNbt(packet.snapshot); }
    static OpenAdminMissionPacket decode(FriendlyByteBuf buffer) { CompoundTag tag = buffer.readNbt(); return new OpenAdminMissionPacket(tag == null ? MissionBoardSnapshot.empty().toTag() : tag); }
    static void handle(OpenAdminMissionPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get(); context.enqueueWork(() -> ClientHooks.acceptAdminMission(packet.snapshot)); context.setPacketHandled(true);
    }
}
