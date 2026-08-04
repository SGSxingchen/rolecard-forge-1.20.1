package com.rolecard.ci;

import com.rolecard.RoleCardMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

/**
 * 仅由 GitHub Actions 的 runClient 加载，确认主菜单稳定后请求 Minecraft 主循环正常停止。
 * 该类位于 src/ci，不属于 main source set，发布 Jar 不会包含它。
 */
@Mod.EventBusSubscriber(modid = RoleCardMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientSmokeProbe {
    private static int titleScreenTicks;
    private static boolean stopping;

    /** 由已被 Forge 扫描到的 RoleCardClient 在 CI 开发运行时反射调用。 */
    public static void install() {
        MinecraftForge.EVENT_BUS.register(ClientSmokeProbe.class);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (stopping || event.phase != TickEvent.Phase.END) return;
        if (Minecraft.getInstance().screen instanceof TitleScreen) {
            titleScreenTicks++;
            if (titleScreenTicks >= 40) {
                stopping = true;
                RoleCardMod.LOGGER.info("ROLECARD_CI_TITLE_SCREEN_READY: 主菜单已稳定 40 tick，正常退出客户端。");
                Minecraft.getInstance().stop();
            }
        } else {
            titleScreenTicks = 0;
        }
    }

    private ClientSmokeProbe() {}
}
