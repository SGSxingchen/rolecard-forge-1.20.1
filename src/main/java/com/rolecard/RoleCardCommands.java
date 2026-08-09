package com.rolecard;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.rolecard.data.CharacterCard;
import com.rolecard.data.RoleCardCapability;
import com.rolecard.data.StatType;
import com.rolecard.network.RoleCardNetwork;
import com.rolecard.mission.MissionBoardSnapshot;
import com.rolecard.mission.MissionObjective;
import com.rolecard.mission.MissionStatus;
import com.rolecard.review.ReviewQueueSavedData;
import com.rolecard.service.RoleCardService;
import com.rolecard.service.MissionBoardService;
import java.util.ArrayList;
import java.util.List;
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
        root.then(mission());
        root.then(Commands.literal("reset").requires(s->s.hasPermission(2)).then(Commands.argument("player",EntityArgument.player()).executes(c->{ServerPlayer p=EntityArgument.getPlayer(c,"player");p.getCapability(RoleCardCapability.CAPABILITY).ifPresent(card->{card.resetAll();RoleCardEvents.syncAndApply(p);});success(c,"已重置角色卡。");return 1;}))); event.getDispatcher().register(root);
    }
    private LiteralArgumentBuilder<CommandSourceStack> mission() {
        LiteralArgumentBuilder<CommandSourceStack> root=Commands.literal("mission");
        root.then(Commands.literal("view").executes(this::missionView));
        LiteralArgumentBuilder<CommandSourceStack> admin=Commands.literal("edit").requires(s->s.hasPermission(2)).executes(this::missionEdit);
        root.then(admin);
        root.then(Commands.literal("clear").requires(s->s.hasPermission(2)).executes(this::missionClear));
        root.then(Commands.literal("status").requires(s->s.hasPermission(2)).then(Commands.argument("status",StringArgumentType.word()).suggests((c,b)->{b.suggest("draft");b.suggest("active");b.suggest("completed");b.suggest("closed");return b.buildFuture();}).executes(this::missionStatus)));
        root.then(Commands.literal("title").requires(s->s.hasPermission(2)).then(Commands.argument("text",StringArgumentType.greedyString()).executes(c->missionText(c,"title"))));
        root.then(Commands.literal("summary").requires(s->s.hasPermission(2)).then(Commands.argument("text",StringArgumentType.greedyString()).executes(c->missionText(c,"summary"))));
        root.then(Commands.literal("instance").requires(s->s.hasPermission(2)).then(Commands.argument("text",StringArgumentType.greedyString()).executes(c->missionText(c,"instance"))));
        LiteralArgumentBuilder<CommandSourceStack> objective=Commands.literal("objective").requires(s->s.hasPermission(2));
        objective.then(Commands.literal("add").then(Commands.argument("text",StringArgumentType.greedyString()).executes(this::objectiveAdd)));
        objective.then(Commands.literal("set").then(Commands.argument("index",IntegerArgumentType.integer(1,MissionBoardSnapshot.MAX_OBJECTIVES)).then(Commands.argument("completed",BoolArgumentType.bool()).executes(c->objectiveSet(c,null)).then(Commands.argument("text",StringArgumentType.greedyString()).executes(c->objectiveSet(c,StringArgumentType.getString(c,"text")))))));
        objective.then(Commands.literal("remove").then(Commands.argument("index",IntegerArgumentType.integer(1,MissionBoardSnapshot.MAX_OBJECTIVES)).executes(this::objectiveRemove)));
        root.then(objective); return root;
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
    private int missionView(CommandContext<CommandSourceStack> c) {
        MissionBoardSnapshot snapshot=MissionBoardService.getSnapshot(c.getSource().getServer());
        ServerPlayer player=c.getSource().getPlayer();
        if(player!=null) { MissionBoardService.sync(player); RoleCardNetwork.openMission(player); }
        c.getSource().sendSuccess(()->Component.literal(snapshot.hasMission()?"已打开世界任务页。":"当前没有已发布的世界任务。"),false); return 1;
    }
    private int missionEdit(CommandContext<CommandSourceStack> c) {
        ServerPlayer player=c.getSource().getPlayer();
        if(player==null){c.getSource().sendFailure(Component.literal("该命令只能由在线管理员执行，以打开任务管理面板。"));return 0;}
        RoleCardNetwork.openAdminMission(player,MissionBoardService.getSnapshot(player.server)); return 1;
    }
    private int missionClear(CommandContext<CommandSourceStack> c) { return missionResult(c,MissionBoardService.clear(c.getSource().getServer(),editor(c)),"已清空世界任务公告。","当前没有可清空的世界任务公告。"); }
    private int missionStatus(CommandContext<CommandSourceStack> c) {
        String raw=StringArgumentType.getString(c,"status"); MissionStatus status;
        if(!raw.equals("draft")&&!raw.equals("active")&&!raw.equals("completed")&&!raw.equals("closed")){c.getSource().sendFailure(Component.literal("状态仅支持 draft、active、completed 或 closed。"));return 0;}
        status=MissionStatus.fromStored(raw);
        return missionResult(c,MissionBoardService.update(c.getSource().getServer(),editor(c),board->board.withStatus(status)),"已更新世界任务状态。","世界任务状态没有变化。");
    }
    private int missionText(CommandContext<CommandSourceStack> c,String field) {
        String text=StringArgumentType.getString(c,"text"); int max=switch(field){case "title"->MissionBoardSnapshot.MAX_TITLE_LENGTH;case "summary"->MissionBoardSnapshot.MAX_SUMMARY_LENGTH;default->MissionBoardSnapshot.MAX_INSTANCE_NAME_LENGTH;}; boolean multiline="summary".equals(field);
        if(!MissionBoardSnapshot.isValid(text,max,multiline)){c.getSource().sendFailure(Component.literal("文本包含控制/格式字符或超过允许长度。"));return 0;}
        return missionResult(c,MissionBoardService.update(c.getSource().getServer(),editor(c),board->"title".equals(field)?board.withTitle(text):"summary".equals(field)?board.withSummary(text):board.withInstanceName(text)),"已更新世界任务资料。","世界任务资料没有变化。");
    }
    private int objectiveAdd(CommandContext<CommandSourceStack> c) {
        String text=StringArgumentType.getString(c,"text"); if(!MissionBoardSnapshot.isValid(text,MissionBoardSnapshot.MAX_OBJECTIVE_LENGTH,false)){c.getSource().sendFailure(Component.literal("目标文本包含控制/格式字符或超过长度限制。"));return 0;}
        MissionBoardSnapshot current=MissionBoardService.getSnapshot(c.getSource().getServer()); if(current.objectives().size()>=MissionBoardSnapshot.MAX_OBJECTIVES){c.getSource().sendFailure(Component.literal("目标数量已达到上限。"));return 0;}
        return missionResult(c,MissionBoardService.update(c.getSource().getServer(),editor(c),board->{List<MissionObjective> list=new ArrayList<>(board.objectives());if(list.size()<MissionBoardSnapshot.MAX_OBJECTIVES)list.add(new MissionObjective(false,text));return board.withObjectives(list);}),"已新增主要目标。","主要目标没有变化。");
    }
    private int objectiveSet(CommandContext<CommandSourceStack> c,String replacement) {
        int index=IntegerArgumentType.getInteger(c,"index")-1; boolean completed=BoolArgumentType.getBool(c,"completed"); MissionBoardSnapshot current=MissionBoardService.getSnapshot(c.getSource().getServer());
        if(index<0||index>=current.objectives().size()){c.getSource().sendFailure(Component.literal("目标序号不存在。"));return 0;}
        if(replacement!=null&&!MissionBoardSnapshot.isValid(replacement,MissionBoardSnapshot.MAX_OBJECTIVE_LENGTH,false)){c.getSource().sendFailure(Component.literal("目标文本包含控制/格式字符或超过长度限制。"));return 0;}
        final String text=replacement; return missionResult(c,MissionBoardService.update(c.getSource().getServer(),editor(c),board->{if(index>=board.objectives().size())return board;List<MissionObjective> list=new ArrayList<>(board.objectives());MissionObjective old=list.get(index);list.set(index,new MissionObjective(completed,text==null?old.text():text));return board.withObjectives(list);}),"已更新主要目标。","主要目标没有变化。");
    }
    private int objectiveRemove(CommandContext<CommandSourceStack> c) {
        int index=IntegerArgumentType.getInteger(c,"index")-1; MissionBoardSnapshot current=MissionBoardService.getSnapshot(c.getSource().getServer());if(index<0||index>=current.objectives().size()){c.getSource().sendFailure(Component.literal("目标序号不存在。"));return 0;}
        return missionResult(c,MissionBoardService.update(c.getSource().getServer(),editor(c),board->{if(index>=board.objectives().size())return board;List<MissionObjective> list=new ArrayList<>(board.objectives());list.remove(index);return board.withObjectives(list);}),"已删除主要目标。","主要目标没有变化。");
    }
    private int missionResult(CommandContext<CommandSourceStack> c,MissionBoardService.MutationResult result,String changed,String same){if(result.changed())success(c,changed);else c.getSource().sendSuccess(()->Component.literal(same),false);return 1;}
    private static String editor(CommandContext<CommandSourceStack> c){ServerPlayer player=c.getSource().getPlayer();return player==null?"控制台":player.getGameProfile().getName();}
    private static void success(CommandContext<CommandSourceStack> c,String text){c.getSource().sendSuccess(()->Component.literal(text),true);}
}
