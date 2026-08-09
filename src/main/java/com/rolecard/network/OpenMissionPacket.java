package com.rolecard.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** 由 /rolecard mission view 或公告点击触发；只通知客户端打开只读页。 */
public final class OpenMissionPacket {
    static void encode(OpenMissionPacket packet, FriendlyByteBuf buffer) { }
    static OpenMissionPacket decode(FriendlyByteBuf buffer) { return new OpenMissionPacket(); }
    static void handle(OpenMissionPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get(); context.enqueueWork(ClientHooks::acceptMissionOpen); context.setPacketHandled(true);
    }
}
