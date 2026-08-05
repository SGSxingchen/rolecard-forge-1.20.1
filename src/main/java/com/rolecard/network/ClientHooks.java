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
    @FunctionalInterface public interface AdminConsumer { void accept(UUID id, String name, CompoundTag card); }
    public static void install(Consumer<CompoundTag> consumer) { cardConsumer = consumer; }
    public static void acceptCard(CompoundTag data) { cardConsumer.accept(data); }
    public static void installPublicName(java.util.function.BiConsumer<UUID, String> consumer) { publicNameConsumer = consumer; }
    public static void acceptPublicName(UUID id, String name) { publicNameConsumer.accept(id, name); }
    public static void installAdmin(AdminConsumer consumer) { adminConsumer = consumer; }
    public static void acceptAdmin(UUID id, String name, CompoundTag card) { adminConsumer.accept(id, name, card); }
    public static void installFeedback(java.util.function.BiConsumer<String, Boolean> consumer) { feedbackConsumer = consumer; }
    public static void acceptFeedback(String message, boolean error) { feedbackConsumer.accept(message, error); }
    private ClientHooks() {}
}
