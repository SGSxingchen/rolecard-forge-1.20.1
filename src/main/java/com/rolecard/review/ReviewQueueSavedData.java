package com.rolecard.review;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** 跨管理员登录保留的待审核轻量索引；角色卡正文仍在玩家 Capability 中。 */
public final class ReviewQueueSavedData extends SavedData {
    private static final String ID = "rolecard_review_queue";
    private final Map<UUID, Entry> entries = new LinkedHashMap<>();
    public record Entry(UUID id, String playerName, String roleName, long submittedAt) {}
    public static ReviewQueueSavedData get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(ReviewQueueSavedData::load, ReviewQueueSavedData::new, ID); }
    public static ReviewQueueSavedData load(CompoundTag tag) {
        ReviewQueueSavedData data = new ReviewQueueSavedData();
        if (!tag.contains("entries", Tag.TAG_LIST)) return data;
        for (Tag item : tag.getList("entries", Tag.TAG_COMPOUND)) { CompoundTag entry = (CompoundTag)item; try { UUID id = entry.getUUID("id"); data.entries.put(id, new Entry(id, entry.getString("playerName"), entry.getString("roleName"), entry.getLong("submittedAt"))); } catch (Exception ignored) {} }
        return data;
    }
    public void put(UUID id, String playerName, String roleName, long at) { entries.put(id, new Entry(id, playerName, roleName, at)); setDirty(); }
    public void remove(UUID id) { if (entries.remove(id) != null) setDirty(); }
    public int size() { return entries.size(); }
    public Iterable<Entry> entries() { return java.util.List.copyOf(entries.values()); }
    @Override public CompoundTag save(CompoundTag tag) { ListTag list = new ListTag(); for (Entry e : entries.values()) { CompoundTag row = new CompoundTag(); row.putUUID("id", e.id()); row.putString("playerName", e.playerName()); row.putString("roleName", e.roleName()); row.putLong("submittedAt", e.submittedAt()); list.add(row); } tag.put("entries", list); return tag; }
}
