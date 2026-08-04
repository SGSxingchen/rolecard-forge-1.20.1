package com.rolecard;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.rolecard.data.RoleCardCapability;
import com.rolecard.data.StatType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class RoleCardCommands {
    @SubscribeEvent
    public void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("rolecard");
        root.then(Commands.literal("card").executes(context -> show(context, context.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player()).requires(source -> source.hasPermission(2)).executes(context -> show(context, EntityArgument.getPlayer(context, "player")))));
        root.then(Commands.literal("identity").then(Commands.literal("set").requires(source -> source.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("age", IntegerArgumentType.integer(0, 999))
                        .then(Commands.argument("gender", StringArgumentType.string()).then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(this::setIdentity)))))));
        root.then(Commands.literal("stat").requires(source -> source.hasPermission(2))
                .then(Commands.literal("set").then(statPlayerValue("set")))
                .then(Commands.literal("add").then(statPlayerValue("add")))
                .then(Commands.literal("reset").then(Commands.argument("player", EntityArgument.player()).executes(context -> {
                    ServerPlayer player = EntityArgument.getPlayer(context, "player");
                    player.getCapability(RoleCardCapability.CAPABILITY).ifPresent(card -> { card.resetStats(); RoleCardEvents.syncAndApply(player); });
                    context.getSource().sendSuccess(() -> Component.literal("已重置 " + player.getName().getString() + " 的六维属性。"), true); return 1;
                }))));
        root.then(Commands.literal("reset").requires(source -> source.hasPermission(2)).then(Commands.argument("player", EntityArgument.player()).executes(context -> {
            ServerPlayer player = EntityArgument.getPlayer(context, "player");
            player.getCapability(RoleCardCapability.CAPABILITY).ifPresent(card -> { card.resetAll(); RoleCardEvents.syncAndApply(player); });
            context.getSource().sendSuccess(() -> Component.literal("已重置 " + player.getName().getString() + " 的角色卡。"), true); return 1;
        })));
        event.getDispatcher().register(root);
    }

    private RequiredArgumentBuilder<CommandSourceStack, EntitySelector> statPlayerValue(String mode) {
        return Commands.argument("player", EntityArgument.player()).then(Commands.argument("stat", StringArgumentType.word()).suggests((context, builder) -> {
            for (StatType type : StatType.values()) builder.suggest(type.key()); return builder.buildFuture();
        }).then(Commands.argument("value", IntegerArgumentType.integer(-100, 100)).executes(context -> changeStat(context, mode))));
    }

    private int setIdentity(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        boolean[] success = {false};
        player.getCapability(RoleCardCapability.CAPABILITY).ifPresent(card -> success[0] = card.setIdentity(StringArgumentType.getString(context, "name"), IntegerArgumentType.getInteger(context, "age"), StringArgumentType.getString(context, "gender")));
        if (!success[0]) throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(Component.literal("身份信息不合法。")).create();
        RoleCardEvents.syncAndApply(player); context.getSource().sendSuccess(() -> Component.literal("已更新身份卡。"), true); return 1;
    }

    private int changeStat(CommandContext<CommandSourceStack> context, String mode) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        StatType type = StatType.fromKey(StringArgumentType.getString(context, "stat"));
        if (type == null) throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(Component.literal("未知六维属性。")).create();
        ServerPlayer player = EntityArgument.getPlayer(context, "player"); int value = IntegerArgumentType.getInteger(context, "value");
        player.getCapability(RoleCardCapability.CAPABILITY).ifPresent(card -> { if ("set".equals(mode)) card.setStat(type, value); else card.addStat(type, value); RoleCardEvents.syncAndApply(player); });
        context.getSource().sendSuccess(() -> Component.literal("已更新 " + player.getName().getString() + " 的" + type.displayName() + "。"), true); return 1;
    }

    private int show(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        player.getCapability(RoleCardCapability.CAPABILITY).ifPresent(card -> {
            context.getSource().sendSuccess(() -> Component.literal("角色：" + card.shownName(player.getGameProfile().getName()) + "｜年龄：" + card.age() + "｜性别：" + card.gender()), false);
            for (StatType type : StatType.values()) context.getSource().sendSuccess(() -> Component.literal(type.displayName() + "：" + card.stat(type)), false);
        }); return 1;
    }
}
