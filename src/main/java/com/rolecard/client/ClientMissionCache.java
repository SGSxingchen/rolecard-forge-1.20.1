package com.rolecard.client;

import java.util.List;
import com.rolecard.mission.MissionBoardSnapshot;
import net.minecraft.nbt.CompoundTag;

/**
 * 客户端只读的世界任务公告快照。它只接受服务端 S2C 数据，不暴露任何本地写入入口。
 * 网络 DTO 的校验、长度限制与 revision 判定均在服务端完成；这里保留一份用于 Screen 绘制的副本。
 */
public final class ClientMissionCache {
    private static MissionBoardSnapshot snapshot = MissionBoardSnapshot.empty();

    /** revision 单调覆盖，延迟 S2C 包不能把已看到的新公告回退为旧版本。 */
    public static void update(CompoundTag data) {
        MissionBoardSnapshot incoming = data == null ? MissionBoardSnapshot.empty() : MissionBoardSnapshot.fromTag(data);
        if (incoming.revision() >= snapshot.revision()) snapshot = incoming;
    }
    public static MissionView mission() { return MissionView.from(snapshot); }

    public record Objective(String text, boolean completed) {}
    public record MissionView(boolean published, String title, String summary, List<Objective> objectives, String rules,
                              String notes, String instanceName, String instanceCode, String difficulty,
                              String playerCountText, String timeLimitText, String status, long revision,
                              long updatedAt, String editorSummary) {
        private static MissionView from(MissionBoardSnapshot data) {
            List<Objective> objectiveList = data.objectives().stream().map(value -> new Objective(value.text(), value.completed())).toList();
            return new MissionView(data.hasMission(), data.title(), data.summary(), objectiveList,
                    data.rules(), data.notes(), data.instanceName(), data.instanceCode(), data.difficulty(),
                    data.playerCountText(), data.timeLimitText(), data.status().name(), data.revision(),
                    data.updatedAt(), data.lastEditor());
        }
    }
    private ClientMissionCache() {}
}
