package com.rolecard;

import com.rolecard.attribute.AttributeApplier;
import com.rolecard.config.RoleCardConfig;
import com.rolecard.data.CharacterCard;
import com.rolecard.data.RoleCardCapability;
import com.rolecard.network.RoleCardNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.NameFormat;
import net.minecraftforge.event.entity.player.PlayerEvent.TabListNameFormat;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class RoleCardEvents {
    public static void registerCapability(RegisterCapabilitiesEvent event) { event.register(CharacterCard.class); }

    public static void configReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != RoleCardConfig.SPEC || ServerLifecycleHooks.getCurrentServer() == null) return;
        for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) syncAndApply(player);
    }

    @SubscribeEvent
    public void attach(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            RoleCardCapability.Provider provider = new RoleCardCapability.Provider();
            event.addCapability(RoleCardCapability.ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public void clone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(RoleCardCapability.CAPABILITY).ifPresent(oldCard -> event.getEntity().getCapability(RoleCardCapability.CAPABILITY).ifPresent(card -> card.copyFrom(oldCard)));
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public void login(PlayerEvent.PlayerLoggedInEvent event) { syncAndApply(event.getEntity()); }

    @SubscribeEvent
    public void respawn(PlayerEvent.PlayerRespawnEvent event) { syncAndApply(event.getEntity()); }

    @SubscribeEvent
    public void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { syncAndApply(event.getEntity()); }

    @SubscribeEvent
    public void name(NameFormat event) {
        if (!RoleCardConfig.SHOW_CHAT_NAME.get()) return;
        event.getEntity().getCapability(RoleCardCapability.CAPABILITY).ifPresent(card -> {
            if (!card.roleName().isBlank()) event.setDisplayname(Component.literal(card.roleName()));
        });
    }

    @SubscribeEvent
    public void tab(TabListNameFormat event) {
        if (RoleCardConfig.TAB_NAME_MODE.get() == RoleCardConfig.TabNameMode.VANILLA) return;
        event.getEntity().getCapability(RoleCardCapability.CAPABILITY).ifPresent(card -> {
            if (RoleCardConfig.TAB_NAME_MODE.get() == RoleCardConfig.TabNameMode.HIDDEN) event.setDisplayName(Component.empty());
            else if (!card.roleName().isBlank()) event.setDisplayName(Component.literal(card.roleName()));
        });
    }

    @SubscribeEvent
    public void tracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer recipient && event.getTarget() instanceof Player target) {
            target.getCapability(RoleCardCapability.CAPABILITY).ifPresent(card -> RoleCardNetwork.syncPublicName(recipient, target, card, RoleCardConfig.SHOW_OVERHEAD_NAME.get()));
        }
    }

    public static void syncAndApply(Player player) {
        if (player instanceof ServerPlayer serverPlayer) serverPlayer.getCapability(RoleCardCapability.CAPABILITY).ifPresent(card -> {
            AttributeApplier.apply(serverPlayer, card);
            serverPlayer.refreshDisplayName();
            serverPlayer.refreshTabListName();
            RoleCardNetwork.sync(serverPlayer, card);
            RoleCardNetwork.syncPublicName(serverPlayer, card, RoleCardConfig.SHOW_OVERHEAD_NAME.get());
        });
    }
}
