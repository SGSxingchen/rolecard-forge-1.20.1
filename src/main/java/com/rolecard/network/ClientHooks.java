package com.rolecard.network;

import net.minecraft.nbt.CompoundTag;
import java.util.function.Consumer;
import java.util.UUID;

/** Client wiring is installed only by the client bootstrap class. */
public final class ClientHooks {
    private static Consumer<CompoundTag> cardConsumer = ignored -> {};
    private static java.util.function.BiConsumer<UUID, String> publicNameConsumer = (id, name) -> {};
    public static void install(Consumer<CompoundTag> consumer) { cardConsumer = consumer; }
    public static void acceptCard(CompoundTag data) { cardConsumer.accept(data); }
    public static void installPublicName(java.util.function.BiConsumer<UUID, String> consumer) { publicNameConsumer = consumer; }
    public static void acceptPublicName(UUID id, String name) { publicNameConsumer.accept(id, name); }
    private ClientHooks() {}
}
