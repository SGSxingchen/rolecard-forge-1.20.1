package com.rolecard.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClientDisplayNames {
    private static final Map<UUID, String> NAMES = new HashMap<>();
    public static void update(UUID id, String name) { if (name.isBlank()) NAMES.remove(id); else NAMES.put(id, name); }
    public static String get(UUID id) { return NAMES.get(id); }
    private ClientDisplayNames() {}
}
