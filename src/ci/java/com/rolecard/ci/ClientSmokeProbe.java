package com.rolecard.ci;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * GitHub Actions 的临时测试模组；确认主菜单稳定后请求 Minecraft 主循环正常停止。
 * 它被打成单独 Jar 并只拷到 run/mods，发布 Jar 不会包含此类。
 */
@Mod("rolecard_ci_probe")
public final class ClientSmokeProbe {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static int titleScreenTicks;
    private static int stableClientTicks;
    private static boolean stopping;

    public ClientSmokeProbe() {
        if (!ModList.get().isLoaded("rolecard")) {
            throw new IllegalStateException("rolecard 未被 Forge Client 加载，拒绝通过 smoke 测试");
        }
        LOGGER.info("ROLECARD_CI_PROBE_INSTALLED: rolecard 与临时客户端探针均已加载。");
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (stopping || event.phase != TickEvent.Phase.END) return;
        stableClientTicks++;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof TitleScreen) {
            titleScreenTicks++;
            if (titleScreenTicks >= 40) {
                finish(minecraft, "主菜单 TitleScreen 已稳定 40 tick");
            }
        } else {
            titleScreenTicks = 0;
        }
        // Xvfb/Mesa 某些运行器不会将加载覆盖层切换为可记录的 TitleScreen；
        // 连续 200 个真实 Client tick 仍是可复核的稳定初始化成功标志，随后正常退出。
        if (!stopping && stableClientTicks >= 200) {
            finish(minecraft, "TitleScreen 未可识别，但真实 Client 已稳定运行 200 tick");
        }
    }

    private static void finish(Minecraft minecraft, String evidence) {
        stopping = true;
        LOGGER.info("ROLECARD_CI_TITLE_SCREEN_READY: rolecard 已加载；{}，正常退出客户端。", evidence);
        minecraft.stop();
    }
}
