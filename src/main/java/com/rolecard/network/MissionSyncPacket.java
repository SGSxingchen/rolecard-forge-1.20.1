package com.rolecard.network;

import com.rolecard.mission.MissionBoardSnapshot;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** 服务端权威的任务公告快照；客户端只缓存并展示，较旧 revision 由缓存拒绝。 */
public record MissionSyncPacket(CompoundTag snapshot) {
    static void encode(MissionSyncPacket packet, FriendlyByteBuf buffer) { buffer.writeNbt(packet.snapshot); }
    static MissionSyncPacket decode(FriendlyByteBuf buffer) { CompoundTag tag = buffer.readNbt(); return new MissionSyncPacket(tag == null ? MissionBoardSnapshot.empty().toTag() : tag); }
    static void handle(MissionSyncPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> ClientHooks.acceptMission(packet.snapshot));
        context.setPacketHandled(true);
    }
}
