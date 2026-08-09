package com.rolecard.service;

import com.rolecard.mission.MissionBoardSavedData;
import com.rolecard.mission.MissionBoardSnapshot;
import com.rolecard.network.RoleCardNetwork;
import java.util.function.UnaryOperator;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * 世界任务公告的唯一服务端写入口。必须在服务器线程调用；外部任务模组或未来数据库桥接
 * 应从 getSnapshot/replace/update 接入，而不能直接改 SavedData 或向客户端伪造状态。
 */
public final class MissionBoardService {
    public record MutationResult(MissionBoardSnapshot snapshot, boolean changed, boolean stale) { }
    public static MissionBoardSnapshot getSnapshot(MinecraftServer server) { return MissionBoardSavedData.get(server).snapshot(); }
    public static MutationResult replace(MinecraftServer server, long expectedRevision, MissionBoardSnapshot requested, String editor) {
        MissionBoardSnapshot current = getSnapshot(server);
        if (expectedRevision != current.revision()) return new MutationResult(current, false, true);
        return store(server, current, requested == null ? MissionBoardSnapshot.empty() : requested, editor);
    }
    public static MutationResult update(MinecraftServer server, String editor, UnaryOperator<MissionBoardSnapshot> updater) {
        MissionBoardSnapshot current = getSnapshot(server);
        MissionBoardSnapshot next = updater == null ? current : updater.apply(current);
        return store(server, current, next == null ? current : next, editor);
    }
    public static MutationResult clear(MinecraftServer server, String editor) {
        MissionBoardSnapshot current = getSnapshot(server);
        if (!current.hasMission()) return new MutationResult(current, false, false);
        return store(server, current, MissionBoardSnapshot.empty(), editor);
    }
    public static void sync(ServerPlayer player) { if (player != null) RoleCardNetwork.syncMission(player, getSnapshot(player.server)); }
    private static MutationResult store(MinecraftServer server, MissionBoardSnapshot current, MissionBoardSnapshot requested, String editor) {
        if (current.sameContent(requested)) return new MutationResult(current, false, false);
        MissionBoardSnapshot saved = requested.withRevision(current.revision() + 1L, System.currentTimeMillis(), editor);
        MissionBoardSavedData.get(server).replace(saved);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) RoleCardNetwork.syncMission(player, saved);
        announce(server, saved);
        return new MutationResult(saved, true, false);
    }
    private static void announce(MinecraftServer server, MissionBoardSnapshot snapshot) {
        MutableComponent action = Component.translatable("rolecard.mission.updated.open")
                .setStyle(Style.EMPTY.withColor(0x55DDEE).withUnderlined(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rolecard mission view")));
        Component message = Component.translatable(snapshot.hasMission() ? "rolecard.mission.updated" : "rolecard.mission.cleared").append(action);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) player.sendSystemMessage(message);
    }
    private MissionBoardService() { }
}
