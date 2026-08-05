package com.rolecard.client;

import com.rolecard.attribute.AttributeMappings;
import com.rolecard.attribute.AttributeRule;
import com.rolecard.data.CharacterCard;
import com.rolecard.data.ReviewStatus;
import com.rolecard.data.StatType;
import com.rolecard.network.AdjustPointsPacket;
import com.rolecard.network.RoleCardNetwork;
import com.rolecard.network.SaveDraftPacket;
import com.rolecard.network.SubmitCardPacket;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;

/** 原生三页 Screen：所有数值请求均仅是增减意图，权威结果由同步包回写。 */
public final class RoleCardScreen extends Screen {
    private EditBox nameBox, ageBox, genderBox;
    private MultiLineBiographyBox bioBox;
    private int left, top, cardWidth, page;
    private final int[] pendingChanges = new int[StatType.values().length];
    private long observedRevision;
    private String feedback = "";
    public RoleCardScreen() { super(Component.literal("角色档案")); }
    @Override protected void init() {
        cardWidth=Math.max(300,Math.min(430,width-12)); left=(width-cardWidth)/2;top=Math.max(4,(height-238)/2); CharacterCard c=ClientCardCache.card(); observedRevision=c.revision();
        nameBox=box(left+72,top+48,cardWidth-84,18,"角色名称",c.roleName(),CharacterCard.MAX_TEXT_LENGTH);
        ageBox=box(left+48,top+74,50,18,"年龄",String.valueOf(c.age()),3); genderBox=box(left+142,top+74,cardWidth-154,18,"性别",c.gender(),CharacterCard.MAX_TEXT_LENGTH);
        bioBox=new MultiLineBiographyBox(font,left+12,top+50,cardWidth-24,104,Component.literal("人物生平")); bioBox.setValue(c.biography()); bioBox.setMaxLength(CharacterCard.MAX_BIOGRAPHY_LENGTH); bioBox.setHint(Component.literal("可输入换行；鼠标滚轮滚动。最多 1500 字"));
        addRenderableWidget(nameBox);addRenderableWidget(ageBox);addRenderableWidget(genderBox);addRenderableWidget(bioBox);
        addRenderableWidget(Button.builder(Component.literal("身份"),b->showPage(0)).bounds(left+12,top+30,55,18).build());addRenderableWidget(Button.builder(Component.literal("生平"),b->showPage(1)).bounds(left+70,top+30,55,18).build());addRenderableWidget(Button.builder(Component.literal("六维"),b->showPage(2)).bounds(left+128,top+30,55,18).build());
        addRenderableWidget(Button.builder(Component.literal("保存草稿"),b->save()).bounds(left+cardWidth-174,top+208,78,20).build());addRenderableWidget(Button.builder(Component.literal("提交角色卡"),b->submit()).bounds(left+cardWidth-92,top+208,80,20).build());addRenderableWidget(Button.builder(Component.literal("关闭"),b->onClose()).bounds(left+12,top+208,48,20).build());showPage(0);
    }
    private EditBox box(int x,int y,int w,int h,String hint,String value,int max){EditBox b=new EditBox(font,x,y,w,h,Component.literal(hint));b.setValue(value);b.setMaxLength(max);return b;}
    private void showPage(int value){page=value; boolean identity=value==0;nameBox.visible=identity;nameBox.setFocused(identity);ageBox.visible=identity;genderBox.visible=identity;bioBox.visible=value==1;bioBox.setFocused(value==1);}
    private boolean editable(){return ClientCardCache.card().canPlayerEdit();}
    private void save(){try{RoleCardNetwork.CHANNEL.sendToServer(new SaveDraftPacket(ClientCardCache.card().revision(),nameBox.getValue(),Integer.parseInt(ageBox.getValue()),genderBox.getValue(),bioBox.getValue()));feedback="已提交草稿，等待服务器校验。";}catch(NumberFormatException e){feedback="年龄必须是 0 至 999 的整数。";}}
    private void submit(){RoleCardNetwork.CHANNEL.sendToServer(new SubmitCardPacket(ClientCardCache.card().revision()));feedback="已发送提交请求。";}
    private void adjust(StatType type,int delta){if(!editable()){feedback="待审核或已批准的角色卡不能自行加点。";return;}pendingChanges[type.ordinal()]+=delta;RoleCardNetwork.CHANNEL.sendToServer(new AdjustPointsPacket(ClientCardCache.card().revision(),type.key(),delta));}
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(bioBox.isFocused() && key==257){bioBox.insertText("\n");return true;}return super.keyPressed(key,scan,modifiers);}
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){renderBackground(g);g.fill(left,top,left+cardWidth,top+238,0xEE18202C);g.fill(left+2,top+2,left+cardWidth-2,top+28,0xFF345170);g.drawCenteredString(font,title,left+cardWidth/2,top+10,0xFFF5F1D8);CharacterCard c=ClientCardCache.card();if(c.revision()!=observedRevision){java.util.Arrays.fill(pendingChanges,0);observedRevision=c.revision();}g.drawString(font,"状态："+c.status().displayName()+"　剩余点数："+c.availablePoints()+"　版本："+c.revision(),left+12,top+12,0xFFAED4E6,false);
        if(page==0){g.drawString(font,"身份信息",left+12,top+43,0xFFAED4E6,false);g.drawString(font,"名称",left+14,top+53,0xFFE7E4D5,false);g.drawString(font,"年龄",left+14,top+79,0xFFE7E4D5,false);g.drawString(font,"性别",left+105,top+79,0xFFE7E4D5,false);}
        else if(page==1){g.drawString(font,"人物生平／人设介绍（输入框支持回车；内容会随草稿保存）",left+12,top+42,0xFFAED4E6,false);g.drawString(font,bioBox.getValue().length()+" / "+CharacterCard.MAX_BIOGRAPHY_LENGTH+" 字符",left+12,top+158,0xFFA8B7C5,false);}
        else renderStats(g,mouseX,mouseY,c);
        if(c.status()==ReviewStatus.REJECTED)g.drawString(font,"退回原因："+c.rejectReason(),left+12,top+184,0xFFFFB16E,false);else if(!editable())g.drawString(font,"当前已锁定：等待审核或已批准，管理员可退回/解锁。",left+12,top+184,0xFFFFD27D,false);if(!feedback.isEmpty())g.drawString(font,feedback,left+64,top+214,0xFFFFD27D,false);super.render(g,mouseX,mouseY,partial);}
    private void renderStats(GuiGraphics g,int mx,int my,CharacterCard c){g.drawString(font,"六维加点：+ 消耗 1 点，- 返还 1 点；悬停查看实际增益",left+12,top+43,0xFFAED4E6,false);for(int i=0;i<StatType.values().length;i++){StatType stat=StatType.values()[i];int y=top+61+i*23;g.fill(left+12,y,left+cardWidth-12,y+19,0xFF223043);g.drawString(font,stat.displayName(),left+18,y+6,0xFFF0EBD5,false);g.drawString(font,c.stat(stat)+(pendingChanges[i]==0?"":"（拟"+(pendingChanges[i]>0?"+":"")+pendingChanges[i]+"）"),left+cardWidth-150,y+6,0xFF9FE3BE,false);int px=left+cardWidth-64;g.fill(px,y+2,px+16,y+17,0xFF496176);g.drawString(font,"-",px+5,y+5,0xFFFFFFFF,false);g.fill(px+22,y+2,px+38,y+17,0xFF496176);g.drawString(font,"+",px+27,y+5,0xFFFFFFFF,false);if(mx>=px&&mx<=px+16&&my>=y+2&&my<=y+17)adjustHover(g,stat,c,mx,my);if(mx>=px+22&&mx<=px+38&&my>=y+2&&my<=y+17)adjustHover(g,stat,c,mx,my);}}
    private void adjustHover(GuiGraphics g,StatType stat,CharacterCard c,int x,int y){g.renderTooltip(font,Component.literal(effectText(c,stat)),x,y);}
    @Override public boolean mouseClicked(double x,double y,int button){if(page==2){for(int i=0;i<StatType.values().length;i++){int row=top+61+i*23,px=left+cardWidth-64;if(y>=row+2&&y<=row+17){if(x>=px&&x<=px+16){adjust(StatType.values()[i],-1);return true;}if(x>=px+22&&x<=px+38){adjust(StatType.values()[i],1);return true;}}}}return super.mouseClicked(x,y,button);}
    private String effectText(CharacterCard c,StatType stat){List<String> parts=new ArrayList<>();for(AttributeRule r:AttributeMappings.rules(stat)){Attribute a=r.target().get();parts.add(a.getDescriptionId().replace("attribute.name.generic.","")+" +"+String.format("%.2f",AttributeMappings.amount(c,stat,r)));}return stat.displayName()+"："+String.join("，",parts);}
    @Override public boolean isPauseScreen(){return false;}
}
