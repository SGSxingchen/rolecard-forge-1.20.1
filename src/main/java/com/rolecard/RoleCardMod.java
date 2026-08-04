package com.rolecard;

import com.mojang.logging.LogUtils;
import com.rolecard.config.RoleCardConfig;
import com.rolecard.network.RoleCardNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(RoleCardMod.MOD_ID)
public final class RoleCardMod {
    public static final String MOD_ID = "rolecard";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RoleCardMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, RoleCardConfig.SPEC);
        RoleCardNetwork.register();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(RoleCardEvents::registerCapability);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(RoleCardEvents::configReload);
        MinecraftForge.EVENT_BUS.register(new RoleCardEvents());
        MinecraftForge.EVENT_BUS.register(new RoleCardCommands());
    }
}
