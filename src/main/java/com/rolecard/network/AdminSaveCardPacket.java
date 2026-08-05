package com.rolecard.network;

import com.rolecard.data.CharacterCard;
import com.rolecard.data.RoleCardCapability;
import com.rolecard.data.StatType;
import com.rolecard.service.RoleCardService;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** 管理界面只提交可编辑字段，状态、提交时间与目标身份均由服务端决定。 */
public record AdminSaveCardPacket(UUID target, long revision, String name, int age, String gender, String biography, int points, int[] stats) {
    static void encode(AdminSaveCardPacket p, FriendlyByteBuf b) { b.writeUUID(p.target);b.writeLong(p.revision);b.writeUtf(p.name,CharacterCard.MAX_TEXT_UTF8_BYTES);b.writeInt(p.age);b.writeUtf(p.gender,CharacterCard.MAX_TEXT_UTF8_BYTES);b.writeUtf(p.biography,CharacterCard.MAX_BIOGRAPHY_UTF8_BYTES);b.writeInt(p.points);b.writeVarInt(p.stats.length);for(int n:p.stats)b.writeInt(n); }
    static AdminSaveCardPacket decode(FriendlyByteBuf b) { UUID id=b.readUUID();long rev=b.readLong();String name=b.readUtf(CharacterCard.MAX_TEXT_UTF8_BYTES);int age=b.readInt();String gender=b.readUtf(CharacterCard.MAX_TEXT_UTF8_BYTES);String bio=b.readUtf(CharacterCard.MAX_BIOGRAPHY_UTF8_BYTES);int points=b.readInt();int count=b.readVarInt();if(count<0||count>StatType.values().length) throw new IllegalArgumentException("六维数组长度非法");int[] stats=new int[count];for(int i=0;i<count;i++)stats[i]=b.readInt();return new AdminSaveCardPacket(id,rev,name,age,gender,bio,points,stats); }
    static void handle(AdminSaveCardPacket p,Supplier<NetworkEvent.Context>s){NetworkEvent.Context c=s.get();ServerPlayer admin=c.getSender();if(admin!=null)c.enqueueWork(()->{if(!admin.hasPermissions(2)){admin.sendSystemMessage(Component.literal("没有管理权限。"));return;}ServerPlayer target=admin.server.getPlayerList().getPlayer(p.target);if(target==null){admin.sendSystemMessage(Component.literal("目标玩家当前不在线。"));return;}target.getCapability(RoleCardCapability.CAPABILITY).ifPresent(card->{if(!RoleCardService.adminSave(target,card,p.revision,p.name,p.age,p.gender,p.biography,p.points,p.stats))admin.sendSystemMessage(Component.literal("保存失败：数据非法或版本已变化。"));else RoleCardNetwork.openAdmin(admin,target,card);});});c.setPacketHandled(true);}
}
