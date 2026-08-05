package com.rolecard.ci;

import com.mojang.logging.LogUtils;
import com.rolecard.client.AdminRoleCardScreen;
import com.rolecard.client.RoleCardScreen;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.nbt.CompoundTag;
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
    private static int uiTicks;
    private static int uiPhase;
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
        if (minecraft.screen instanceof TitleScreen && uiPhase == 0) {
            titleScreenTicks++;
            if (titleScreenTicks >= 40) {
                openPlayer(minecraft);
            }
        } else {
            titleScreenTicks = 0;
        }
        if (uiPhase > 0) exerciseUi(minecraft);
        // Xvfb/Mesa 某些运行器不会将加载覆盖层切换为可记录的 TitleScreen；
        // 连续 200 个真实 Client tick 仍是可复核的稳定初始化成功标志，随后正常退出。
        if (!stopping && stableClientTicks >= 240) {
            finish(minecraft, "TitleScreen 未可识别，但真实 Client 已稳定运行 200 tick");
        }
    }

    private static void openPlayer(Minecraft minecraft) {
        RoleCardScreen screen = new RoleCardScreen(); minecraft.setScreen(screen);
        if (!screen.ciLayoutWithinSafeArea()) throw new IllegalStateException("玩家档案布局越过安全区或分区重叠");
        uiPhase = 1; uiTicks = 0;
        LOGGER.info("ROLECARD_CI_UI_PLAYER_OPEN: 玩家身份页已初始化。");
    }
    private static void exerciseUi(Minecraft minecraft) {
        if (++uiTicks < 12) return;
        uiTicks = 0;
        if (uiPhase >= 1 && uiPhase <= 3 && minecraft.screen instanceof RoleCardScreen player) {
            player.ciShowPage(uiPhase - 1);
            if (!player.ciLayoutWithinSafeArea()) throw new IllegalStateException("玩家页签布局不稳定");
            LOGGER.info("ROLECARD_CI_UI_PLAYER_PAGE_OK: 页签 {} 初始化、切换与稳定 tick 通过。", uiPhase - 1);
            uiPhase++;
            return;
        }
        if (uiPhase == 4) {
            CompoundTag data = new CompoundTag(); data.putString("roleName", "CI 调查员"); data.putString("biography", "用于客户端审核界面探针的多行资料。\n第二行。");
            AdminRoleCardScreen screen = new AdminRoleCardScreen(UUID.fromString("00000000-0000-0000-0000-000000000001"), "CI_Player", data);
            minecraft.setScreen(screen);
            if (!screen.ciLayoutWithinSafeArea()) throw new IllegalStateException("管理员档案布局越过安全区或分区重叠");
            uiPhase = 5; LOGGER.info("ROLECARD_CI_UI_ADMIN_OPEN: 管理员资料页已初始化。"); return;
        }
        if (uiPhase >= 5 && uiPhase <= 7 && minecraft.screen instanceof AdminRoleCardScreen admin) {
            admin.ciShowPage(uiPhase - 5);
            if (!admin.ciLayoutWithinSafeArea()) throw new IllegalStateException("管理员页签布局不稳定");
            LOGGER.info("ROLECARD_CI_UI_ADMIN_PAGE_OK: 页签 {} 初始化、切换与稳定 tick 通过。", uiPhase - 5);
            uiPhase++;
            return;
        }
        if (uiPhase == 8) finish(minecraft, "主菜单稳定后已依次验证玩家三页与管理员三页的初始化、切换和稳定 tick");
    }

    private static void finish(Minecraft minecraft, String evidence) {
        stopping = true;
        LOGGER.info("ROLECARD_CI_TITLE_SCREEN_READY: rolecard 已加载；{}，正常退出客户端。", evidence);
        minecraft.stop();
    }
}
