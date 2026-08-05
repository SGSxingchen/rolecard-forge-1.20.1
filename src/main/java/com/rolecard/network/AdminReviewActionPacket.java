package com.rolecard.network;

import com.rolecard.data.ReviewStatus;
import com.rolecard.data.RoleCardCapability;
import com.rolecard.service.RoleCardService;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record AdminReviewActionPacket(UUID target, long revision, String action, String reason) {
    static void encode(AdminReviewActionPacket p, FriendlyByteBuf b) { b.writeUUID(p.target); b.writeLong(p.revision); b.writeUtf(p.action, 16); b.writeUtf(p.reason, 640); }
    static AdminReviewActionPacket decode(FriendlyByteBuf b) { return new AdminReviewActionPacket(b.readUUID(),b.readLong(),b.readUtf(16),b.readUtf(640)); }
    static void handle(AdminReviewActionPacket p, Supplier<NetworkEvent.Context> s) { NetworkEvent.Context c=s.get(); ServerPlayer admin=c.getSender(); if(admin!=null)c.enqueueWork(()->{ if(!admin.hasPermissions(2)) { admin.sendSystemMessage(net.minecraft.network.chat.Component.literal("没有审核权限。")); RoleCardNetwork.feedback(admin, "权限不足：没有审核权限。", true); return; } ServerPlayer target=admin.server.getPlayerList().getPlayer(p.target); if(target==null){admin.sendSystemMessage(net.minecraft.network.chat.Component.literal("目标玩家当前不在线。"));RoleCardNetwork.feedback(admin, "目标玩家当前不在线。", true);return;} target.getCapability(RoleCardCapability.CAPABILITY).ifPresent(card->{if(!RoleCardService.revisionMatches(card,p.revision)){RoleCardService.stale(target);RoleCardNetwork.feedback(admin, "修订号冲突：资料已变化，请重新确认。", true);return;} boolean ok=switch(p.action){case "approve"->RoleCardService.approve(target,card);case "reject"->RoleCardService.reject(target,card,p.reason);case "unlock"->{RoleCardService.unlock(target,card);yield true;}default->false;};if(!ok){admin.sendSystemMessage(net.minecraft.network.chat.Component.literal("当前状态不允许该审核操作。"));RoleCardNetwork.feedback(admin, "当前状态不允许该审核操作。", true);}else RoleCardNetwork.openAdmin(admin,target,card);});}); c.setPacketHandled(true); }
}
