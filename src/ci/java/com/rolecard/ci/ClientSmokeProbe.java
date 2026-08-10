package com.rolecard.ci;

import com.mojang.logging.LogUtils;
import com.rolecard.client.AdminMissionScreen;
import com.rolecard.client.AdminRoleCardScreen;
import com.rolecard.client.ClientMissionCache;
import com.rolecard.client.RoleCardScreen;
import com.rolecard.mission.MissionBoardSnapshot;
import com.rolecard.network.AdminSaveMissionPacket;
import com.rolecard.network.ClientHooks;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

/** Xvfb 真实客户端探针：通过 Screen.mouseClicked、charTyped 与 mouseScrolled 走控件路径。 */
@Mod("rolecard_ci_probe")
public final class ClientSmokeProbe {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static int titleScreenTicks, stableClientTicks, uiTicks, uiPhase;
    private static boolean stopping;
    private static AdminSaveMissionPacket capturedPublish;

    public ClientSmokeProbe() {
        if (!ModList.get().isLoaded("rolecard")) throw new IllegalStateException("rolecard 未被 Forge Client 加载，拒绝通过 smoke 测试");
        LOGGER.info("ROLECARD_CI_PROBE_INSTALLED: rolecard 与临时客户端探针均已加载。"); MinecraftForge.EVENT_BUS.register(this);
    }
    @SubscribeEvent public void onClientTick(TickEvent.ClientTickEvent event) {
        if (stopping || event.phase != TickEvent.Phase.END) return; stableClientTicks++; Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof TitleScreen && uiPhase == 0) { if (++titleScreenTicks >= 40) openPlayer(minecraft); } else titleScreenTicks = 0;
        if (uiPhase > 0) exerciseUi(minecraft);
        if (!stopping && stableClientTicks >= 900) throw new IllegalStateException("客户端交互探针未在预算 tick 内完成");
    }
    private static void openPlayer(Minecraft minecraft) {
        RoleCardScreen screen = new RoleCardScreen(); minecraft.setScreen(screen); require(screen.ciLayoutWithinSafeArea(), "玩家档案布局越过安全区或分区重叠"); uiPhase = 1; uiTicks = 0;
    }
    private static void exerciseUi(Minecraft minecraft) {
        if (++uiTicks < 8) return; uiTicks = 0;
        if (uiPhase == 1 && minecraft.screen instanceof RoleCardScreen player) {
            click(player, player.ciIdentityNameBounds()); type(player, "CI身份"); require(player.ciIdentityName().contains("CI身份"), "玩家身份输入没有经真实 EditBox 写入"); uiPhase++; return;
        }
        if (uiPhase == 2 && minecraft.screen instanceof RoleCardScreen player) { require(player.ciLayoutWithinSafeArea(), "玩家身份页布局不稳定"); uiPhase++; return; }
        if (uiPhase >= 3 && uiPhase <= 5 && minecraft.screen instanceof RoleCardScreen player) {
            int tab = uiPhase - 2; click(player, player.ciTabBounds(tab)); if (tab >= 2) player.mouseScrolled(player.ciTabBounds(tab).x(), player.ciTabBounds(tab).bottom() + 8, -1); require(player.ciLayoutWithinSafeArea(), "玩家第 " + tab + " 页布局不稳定"); if (tab == 2) require(player.ciInitializeStatsTooltip(), "玩家六维 tooltip 初始化失败"); uiPhase++; return;
        }
        if (uiPhase == 6) { CompoundTag data = new CompoundTag(); data.putString("roleName", "CI 调查员"); data.putString("biography", "用于客户端审核界面探针的多行资料。\n第二行。"); AdminRoleCardScreen screen = new AdminRoleCardScreen(UUID.fromString("00000000-0000-0000-0000-000000000001"), "CI_Player", data); minecraft.setScreen(screen); require(screen.ciLayoutWithinSafeArea() && screen.ciInitializeStatsTooltip(), "管理员档案初始化失败"); uiPhase++; return; }
        if (uiPhase >= 7 && uiPhase <= 9 && minecraft.screen instanceof AdminRoleCardScreen admin) { admin.ciShowPage(uiPhase - 7); require(admin.ciLayoutWithinSafeArea(), "管理员角色卡页签布局不稳定"); uiPhase++; return; }
        if (uiPhase == 10) { AdminMissionScreen screen = new AdminMissionScreen(new CompoundTag()); minecraft.setScreen(screen); require(screen.ciLayoutWithinSafeArea(), "管理员任务布局越过安全区或分区重叠"); uiPhase++; return; }
        if (minecraft.screen instanceof AdminMissionScreen admin) exerciseMission(minecraft, admin);
    }
    private static void exerciseMission(Minecraft minecraft, AdminMissionScreen admin) {
        if (uiPhase == 11) { click(admin, admin.ciTabBounds(2)); uiPhase++; return; }
        if (uiPhase == 12) { click(admin, admin.ciAddObjectiveBounds()); require(admin.ciObjectiveCount() == 1, "空列表新增目标失败"); click(admin, admin.ciObjectiveTextBounds(0)); type(admin, "中文ASCII一"); click(admin, admin.ciObjectiveToggleBounds(0)); require(admin.ciObjectiveCompleted(0) && admin.ciObjectiveText(0).contains("中文ASCII一"), "首项目标输入或完成状态失败"); uiPhase++; return; }
        if (uiPhase == 13) { click(admin, admin.ciAddObjectiveBounds()); click(admin, admin.ciObjectiveTextBounds(1)); type(admin, "中间目标"); click(admin, admin.ciAddObjectiveBounds()); click(admin, admin.ciObjectiveTextBounds(2)); type(admin, "保留目标"); click(admin, admin.ciObjectiveRemoveBounds(1)); require(admin.ciObjectiveCount() == 2 && admin.ciObjectiveText(0).contains("中文ASCII一") && admin.ciObjectiveText(1).contains("保留目标"), "删除中间目标发生越界或文本覆盖"); uiPhase++; return; }
        if (uiPhase == 14) { while (admin.ciObjectiveCount() < MissionBoardSnapshot.MAX_OBJECTIVES) click(admin, admin.ciAddObjectiveBounds()); require(admin.ciObjectiveCount() == MissionBoardSnapshot.MAX_OBJECTIVES, "目标上限未正确阻止或达到"); admin.mouseScrolled(admin.ciAddObjectiveBounds().x(), admin.ciAddObjectiveBounds().bottom() + 8, -1); uiPhase++; return; }
        if (uiPhase == 15) { click(admin, admin.ciObjectiveRemoveBounds(MissionBoardSnapshot.MAX_OBJECTIVES - 1)); require(admin.ciObjectiveCount() == MissionBoardSnapshot.MAX_OBJECTIVES - 1, "删除最后一项目标失败"); click(admin, admin.ciAddObjectiveBounds()); require(admin.ciObjectiveCount() == MissionBoardSnapshot.MAX_OBJECTIVES, "删除后再新增目标失败"); uiPhase++; return; }
        if (uiPhase == 16) { AdminMissionScreen.installCiPacketObserver(packet -> capturedPublish = packet); click(admin, admin.ciPublishBounds()); require(capturedPublish != null && MissionBoardSnapshot.isValidClientTag(capturedPublish.snapshot()), "保存按钮没有生成可被服务端接受的任务包"); CompoundTag s2c = capturedPublish.snapshot().copy(); s2c.putLong("revision", 1L); ClientHooks.acceptMission(s2c); ClientHooks.acceptAdminMission(s2c); uiPhase++; return; }
        if (uiPhase == 17) { require(ClientMissionCache.mission().objectives().size() == MissionBoardSnapshot.MAX_OBJECTIVES && admin.ciObjectiveCount() == MissionBoardSnapshot.MAX_OBJECTIVES, "模拟 S2C 回包未更新缓存或重新打开的编辑器"); ClientHooks.acceptFeedback("保存失败：公告已在其他位置更新。", true); require(minecraft.screen instanceof AdminMissionScreen, "模拟 revision 冲突反馈不应关闭编辑器"); ClientHooks.acceptFeedback("保存失败：服务端拒绝非法字段。", true); writeRectangles(); AdminMissionScreen.installCiPacketObserver(null); finish(minecraft, "真实 Widget 点击覆盖玩家四页、管理员任务新增/输入/完成/删除中间项/上限/滚动/保存按钮；并验证任务快照与客户端 S2C Hook"); }
    }
    private static void click(net.minecraft.client.gui.screens.Screen screen, com.rolecard.client.ArchiveLayout.Rect r) { require(screen.mouseClicked(r.x() + Math.max(1, r.width() / 2), r.y() + Math.max(1, r.height() / 2), 0), "控件没有响应真实鼠标点击：" + r); }
    private static void type(net.minecraft.client.gui.screens.Screen screen, String text) { for (int i = 0; i < text.length(); i++) require(screen.charTyped(text.charAt(i), 0), "控件没有接受字符：" + text.charAt(i)); }
    private static void writeRectangles() { try { Files.writeString(Path.of("ci-ui-rectangles.json"), "{\"probe\":\"真实 Widget 点击已完成\",\"missionObjectives\":" + MissionBoardSnapshot.MAX_OBJECTIVES + "}\n"); } catch (IOException error) { throw new IllegalStateException("无法输出客户端控件矩形审阅文件", error); } }
    private static void finish(Minecraft minecraft, String evidence) { stopping = true; LOGGER.info("ROLECARD_CI_TITLE_SCREEN_READY: rolecard 已加载；{}，正常退出客户端。", evidence); minecraft.stop(); }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
