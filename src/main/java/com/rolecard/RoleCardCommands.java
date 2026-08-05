package com.rolecard;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.rolecard.data.CharacterCard;
import com.rolecard.data.RoleCardCapability;
import com.rolecard.data.StatType;
import com.rolecard.network.RoleCardNetwork;
import com.rolecard.review.ReviewQueueSavedData;
import com.rolecard.service.RoleCardService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 在线目标命令；待审索引本身可离线列出，打开面板需要目标客户端在线。 */
public final class RoleCardCommands {
    @SubscribeEvent public void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root=Commands.literal("rolecard");
        root.then(Commands.literal("card").executes(c->show(c,c.getSource().getPlayerOrException())).then(Commands.argument("player",EntityArgument.player()).requires(s->s.hasPermission(2)).executes(c->show(c,EntityArgument.getPlayer(c,"player")))));
        root.then(Commands.literal("identity").then(Commands.literal("set").requires(s->s.hasPermission(2)).then(Commands.argument("player",EntityArgument.player()).then(Commands.argument("age",IntegerArgumentType.integer(0,999)).then(Commands.argument("gender",StringArgumentType.string()).then(Commands.argument("name",StringArgumentType.greedyString()).executes(this::setIdentity)))))));
        root.then(Commands.literal("stat").requires(s->s.hasPermission(2)).then(Commands.literal("set").then(statPlayerValue("set"))).then(Commands.literal("add").then(statPlayerValue("add"))).then(Commands.literal("reset").then(Commands.argument("player",EntityArgument.player()).executes(c->{ServerPlayer p=EntityArgument.getPlayer(c,"player");p.getCapability(RoleCardCapability.CAPABILITY).ifPresent(card->{card.resetStats();RoleCardEvents.syncAndApply(p);});success(c,"已重置六维属性。");return 1;}))));
        root.then(Commands.literal("points").requires(s->s.hasPermission(2)).then(Commands.literal("set").then(Commands.argument("player",EntityArgument.player()).then(Commands.argument("value",IntegerArgumentType.integer(0,100000)).executes(c->points(c,"set"))))).then(Commands.literal("add").then(Commands.argument("player",EntityArgument.player()).then(Commands.argument("value",IntegerArgumentType.integer(-100000,100000)).executes(c->points(c,"add"))))));
        LiteralArgumentBuilder<CommandSourceStack> review=Commands.literal("review").requires(s->s.hasPermission(2));
        review.then(Commands.literal("list").executes(this::list));
        review.then(Commands.literal("open").then(Commands.argument("player",EntityArgument.player()).executes(c->open(c,EntityArgument.getPlayer(c,"player")))));
        review.then(Commands.literal("approve").then(Commands.argument("player",EntityArgument.player()).executes(c->approve(c,EntityArgument.getPlayer(c,"player")))));
        review.then(Commands.literal("unlock").then(Commands.argument("player",EntityArgument.player()).executes(c->unlock(c,EntityArgument.getPlayer(c,"player")))));
        review.then(Commands.literal("reject").then(Commands.argument("player",EntityArgument.player()).executes(c->reject(c,EntityArgument.getPlayer(c,"player"),"未填写原因")).then(Commands.argument("reason",StringArgumentType.greedyString()).executes(c->reject(c,EntityArgument.getPlayer(c,"player"),StringArgumentType.getString(c,"reason")))))); root.then(review);
        root.then(Commands.literal("reset").requires(s->s.hasPermission(2)).then(Commands.argument("player",EntityArgument.player()).executes(c->{ServerPlayer p=EntityArgument.getPlayer(c,"player");p.getCapability(RoleCardCapability.CAPABILITY).ifPresent(card->{card.resetAll();RoleCardEvents.syncAndApply(p);});success(c,"已重置角色卡。");return 1;}))); event.getDispatcher().register(root);
    }
    private RequiredArgumentBuilder<CommandSourceStack,EntitySelector> statPlayerValue(String mode){return Commands.argument("player",EntityArgument.player()).then(Commands.argument("stat",StringArgumentType.word()).suggests((c,b)->{for(StatType t:StatType.values())b.suggest(t.key());return b.buildFuture();}).then(Commands.argument("value",IntegerArgumentType.integer(-100,100)).executes(c->changeStat(c,mode))));}
    private int setIdentity(CommandContext<CommandSourceStack> c)throws com.mojang.brigadier.exceptions.CommandSyntaxException{ServerPlayer p=EntityArgument.getPlayer(c,"player");boolean[] ok={false};p.getCapability(RoleCardCapability.CAPABILITY).ifPresent(card->{ok[0]=card.setIdentity(StringArgumentType.getString(c,"name"),IntegerArgumentType.getInteger(c,"age"),StringArgumentType.getString(c,"gender"));if(ok[0]){card.touch();RoleCardEvents.syncAndApply(p);}});if(!ok[0])throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(Component.literal("身份信息不合法。")).create();success(c,"已更新身份卡。");return 1;}
    private int changeStat(CommandContext<CommandSourceStack> c,String mode)throws com.mojang.brigadier.exceptions.CommandSyntaxException{StatType type=StatType.fromKey(StringArgumentType.getString(c,"stat"));if(type==null)throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(Component.literal("未知六维属性。")).create();ServerPlayer p=EntityArgument.getPlayer(c,"player");int n=IntegerArgumentType.getInteger(c,"value");p.getCapability(RoleCardCapability.CAPABILITY).ifPresent(card->{if("set".equals(mode))card.setStat(type,n);else card.addStat(type,n);card.touch();RoleCardEvents.syncAndApply(p);});success(c,"已更新 "+p.getName().getString()+" 的"+type.displayName()+"。");return 1;}
    private int points(CommandContext<CommandSourceStack> c,String mode)throws com.mojang.brigadier.exceptions.CommandSyntaxException{ServerPlayer p=EntityArgument.getPlayer(c,"player");int n=IntegerArgumentType.getInteger(c,"value");p.getCapability(RoleCardCapability.CAPABILITY).ifPresent(card->{if("set".equals(mode))card.setAvailablePoints(n);else card.addAvailablePoints(n);RoleCardEvents.syncAndApply(p);});success(c,"已调整 "+p.getName().getString()+" 的可分配点数。");return 1;}
    private int list(CommandContext<CommandSourceStack> c){ReviewQueueSavedData q=ReviewQueueSavedData.get(c.getSource().getServer());if(q.size()==0)c.getSource().sendSuccess(()->Component.literal("当前没有待审核角色卡。"),false);else{c.getSource().sendSuccess(()->Component.literal("待审核角色卡："+q.size()+" 张"),false);for(ReviewQueueSavedData.Entry e:q.entries())c.getSource().sendSuccess(()->Component.literal("- "+e.playerName()+"／"+e.roleName()+"（"+e.id()+"）"),false);}return q.size();}
    private int open(CommandContext<CommandSourceStack> c,ServerPlayer target){ServerPlayer admin=c.getSource().getPlayer();if(admin==null){c.getSource().sendFailure(Component.literal("该命令只能由在线管理员执行，以打开管理界面。"));return 0;}target.getCapability(RoleCardCapability.CAPABILITY).ifPresent(card->RoleCardNetwork.openAdmin(admin,target,card));return 1;}
    private int approve(CommandContext<CommandSourceStack> c,ServerPlayer p){boolean[]ok={false};p.getCapability(RoleCardCapability.CAPABILITY).ifPresent(card->ok[0]=RoleCardService.approve(p,card));if(!ok[0]){c.getSource().sendFailure(Component.literal("该角色卡不是待审核状态。"));return 0;}success(c,"已批准角色卡。");return 1;}
    private int reject(CommandContext<CommandSourceStack> c,ServerPlayer p,String reason){boolean[]ok={false};p.getCapability(RoleCardCapability.CAPABILITY).ifPresent(card->ok[0]=RoleCardService.reject(p,card,reason));if(!ok[0]){c.getSource().sendFailure(Component.literal("该角色卡不是待审核状态。"));return 0;}success(c,"已退回角色卡。");return 1;}
    private int unlock(CommandContext<CommandSourceStack> c,ServerPlayer p){p.getCapability(RoleCardCapability.CAPABILITY).ifPresent(card->RoleCardService.unlock(p,card));success(c,"已解锁角色卡。");return 1;}
    private int show(CommandContext<CommandSourceStack> c,ServerPlayer p){p.getCapability(RoleCardCapability.CAPABILITY).ifPresent(card->{c.getSource().sendSuccess(()->Component.literal("角色："+card.shownName(p.getGameProfile().getName())+"｜年龄："+card.age()+"｜性别："+card.gender()+"｜状态："+card.status().displayName()+"｜剩余点数："+card.availablePoints()),false);for(StatType t:StatType.values())c.getSource().sendSuccess(()->Component.literal(t.displayName()+"："+card.stat(t)),false);});return 1;}
    private static void success(CommandContext<CommandSourceStack> c,String text){c.getSource().sendSuccess(()->Component.literal(text),true);}
}
