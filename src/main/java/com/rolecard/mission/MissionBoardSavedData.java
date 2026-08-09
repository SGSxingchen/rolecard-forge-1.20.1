package com.rolecard.mission;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Overworld/data 中的全局公告；不依附玩家 Capability，世界备份即可一并恢复。 */
public final class MissionBoardSavedData extends SavedData {
    private static final String ID = "rolecard_mission_board";
    private MissionBoardSnapshot snapshot = MissionBoardSnapshot.empty();
    public static MissionBoardSavedData get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(MissionBoardSavedData::load, MissionBoardSavedData::new, ID); }
    public static MissionBoardSavedData load(CompoundTag tag) {
        MissionBoardSavedData data = new MissionBoardSavedData();
        try { data.snapshot = MissionBoardSnapshot.fromTag(tag == null ? new CompoundTag() : tag); } catch (RuntimeException ignored) { data.snapshot = MissionBoardSnapshot.empty(); }
        return data;
    }
    public MissionBoardSnapshot snapshot() { return snapshot; }
    public void replace(MissionBoardSnapshot next) { snapshot = next == null ? MissionBoardSnapshot.empty() : next; setDirty(); }
    @Override public CompoundTag save(CompoundTag tag) { return snapshot.toTag(); }
}
