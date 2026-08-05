package com.rolecard.client;

import com.rolecard.data.CharacterCard;
import com.rolecard.data.StatType;
import com.rolecard.network.AdminReviewActionPacket;
import com.rolecard.network.AdminSaveCardPacket;
import com.rolecard.network.RoleCardNetwork;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

/** 收到 S2C DTO 后打开；该界面绝不直接读写其他玩家本地数据。 */
public final class AdminRoleCardScreen extends Screen {
    private final UUID target; private final String originalName; private final CharacterCard card=new CharacterCard();
    private EditBox name,age,gender,bio,points,reason; private final EditBox[] stats=new EditBox[StatType.values().length]; private int left,top,w;
    public AdminRoleCardScreen(UUID target,String originalName,CompoundTag data){super(Component.literal("角色卡管理"));this.target=target;this.originalName=originalName;if(data!=null)card.load(data);}
    @Override protected void init(){w=Math.max(300,Math.min(440,width-12));left=(width-w)/2;top=Math.max(3,(height-238)/2);name=box(left+58,top+47,w-70,18,card.roleName(),32);age=box(left+42,top+70,48,18,String.valueOf(card.age()),3);gender=box(left+130,top+70,w-142,18,card.gender(),32);bio=box(left+12,top+94,w-24,18,card.biography(),CharacterCard.MAX_BIOGRAPHY_LENGTH);points=box(left+72,top+117,55,18,String.valueOf(card.availablePoints()),6);reason=box(left+12,top+192,w-24,18,"",160);reason.setHint(Component.literal("退回原因（可选）"));addRenderableWidget(name);addRenderableWidget(age);addRenderableWidget(gender);addRenderableWidget(bio);addRenderableWidget(points);addRenderableWidget(reason);for(int i=0;i<stats.length;i++){int col=i%3,row=i/3;stats[i]=box(left+60+col*(w/3),top+142+row*22,w/3-68,18,String.valueOf(card.stat(StatType.values()[i])),3);addRenderableWidget(stats[i]);}addRenderableWidget(Button.builder(Component.literal("保存"),b->save()).bounds(left+12,top+214,45,20).build());addRenderableWidget(Button.builder(Component.literal("批准"),b->action("approve")).bounds(left+w-153,top+214,45,20).build());addRenderableWidget(Button.builder(Component.literal("退回"),b->action("reject")).bounds(left+w-105,top+214,45,20).build());addRenderableWidget(Button.builder(Component.literal("解锁"),b->action("unlock")).bounds(left+w-57,top+214,45,20).build());}
    private EditBox box(int x,int y,int width,int height,String value,int max){EditBox b=new EditBox(font,x,y,width,height,Component.empty());b.setValue(value);b.setMaxLength(max);return b;}
    private void save(){try{int[] values=new int[stats.length];for(int i=0;i<values.length;i++)values[i]=Integer.parseInt(stats[i].getValue());RoleCardNetwork.CHANNEL.sendToServer(new AdminSaveCardPacket(target,card.revision(),name.getValue(),Integer.parseInt(age.getValue()),gender.getValue(),bio.getValue(),Integer.parseInt(points.getValue()),values));}catch(NumberFormatException ignored){}}
    private void action(String action){RoleCardNetwork.CHANNEL.sendToServer(new AdminReviewActionPacket(target,card.revision(),action,reason.getValue()));}
    @Override public void render(GuiGraphics g,int mx,int my,float p){renderBackground(g);g.fill(left,top,left+w,top+238,0xEE18202C);g.fill(left+2,top+2,left+w-2,top+28,0xFF345170);g.drawCenteredString(font,"管理角色卡："+originalName,left+w/2,top+10,0xFFF5F1D8);g.drawString(font,"状态："+card.status().displayName()+"　版本："+card.revision(),left+12,top+32,0xFFAED4E6,false);g.drawString(font,"名称",left+12,top+52,0xFFE7E4D5,false);g.drawString(font,"年龄",left+12,top+75,0xFFE7E4D5,false);g.drawString(font,"性别",left+98,top+75,0xFFE7E4D5,false);g.drawString(font,"生平（单行编辑；完整内容按原样保存）",left+12,top+84,0xFFAED4E6,false);g.drawString(font,"剩余点数",left+12,top+122,0xFFE7E4D5,false);for(int i=0;i<stats.length;i++){int col=i%3,row=i/3;g.drawString(font,StatType.values()[i].displayName(),left+12+col*(w/3),top+147+row*22,0xFFE7E4D5,false);}super.render(g,mx,my,p);}
    @Override public boolean isPauseScreen(){return false;}
}
