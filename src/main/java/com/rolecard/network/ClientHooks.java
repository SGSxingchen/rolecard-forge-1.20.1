package com.rolecard.network;

import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
/** Client wiring is installed only by the client bootstrap class. */
public final class ClientHooks {
    private static Consumer<CompoundTag> cardConsumer = ignored -> {};
    private static java.util.function.BiConsumer<UUID, String> publicNameConsumer = (id, name) -> {};
    private static AdminConsumer adminConsumer = (id, name, card) -> {};
    private static java.util.function.BiConsumer<String, Boolean> feedbackConsumer = (message, error) -> {};
    private static Consumer<CompoundTag> missionConsumer = ignored -> {};
    private static Consumer<CompoundTag> adminMissionConsumer = ignored -> {};
    private static Runnable missionOpenConsumer = () -> {};
    @FunctionalInterface public interface AdminConsumer { void accept(UUID id, String name, CompoundTag card); }
    public static void install(Consumer<CompoundTag> consumer) { cardConsumer = consumer; }
    public static void acceptCard(CompoundTag data) { cardConsumer.accept(data); }
    public static void installPublicName(java.util.function.BiConsumer<UUID, String> consumer) { publicNameConsumer = consumer; }
    public static void acceptPublicName(UUID id, String name) { publicNameConsumer.accept(id, name); }
    public static void installAdmin(AdminConsumer consumer) { adminConsumer = consumer; }
    public static void acceptAdmin(UUID id, String name, CompoundTag card) { adminConsumer.accept(id, name, card); }
    public static void installFeedback(java.util.function.BiConsumer<String, Boolean> consumer) { feedbackConsumer = consumer; }
    public static void acceptFeedback(String message, boolean error) { feedbackConsumer.accept(message, error); }
    public static void installMission(Consumer<CompoundTag> consumer) { missionConsumer = consumer == null ? ignored -> {} : consumer; }
    public static void acceptMission(CompoundTag snapshot) { missionConsumer.accept(snapshot == null ? new CompoundTag() : snapshot); }
    public static void installAdminMission(Consumer<CompoundTag> consumer) { adminMissionConsumer = consumer == null ? ignored -> {} : consumer; }
    public static void acceptAdminMission(CompoundTag snapshot) { adminMissionConsumer.accept(snapshot == null ? new CompoundTag() : snapshot); }
    public static void installMissionOpen(Runnable consumer) { missionOpenConsumer = consumer == null ? () -> {} : consumer; }
    public static void acceptMissionOpen() { missionOpenConsumer.run(); }
    private ClientHooks() {}
}
