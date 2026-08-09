package com.rolecard.mission;

/** 目标完成状态只由管理员或外部受信服务写入，模组不自动判定。 */
public record MissionObjective(boolean completed, String text) {
    public MissionObjective {
        text = MissionBoardSnapshot.clean(text, MissionBoardSnapshot.MAX_OBJECTIVE_LENGTH, false);
    }
}
