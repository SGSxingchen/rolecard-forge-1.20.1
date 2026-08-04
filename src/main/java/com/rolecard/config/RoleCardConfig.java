package com.rolecard.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class RoleCardConfig {
    public enum TabNameMode { VANILLA, ROLE, HIDDEN }

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue SHOW_OVERHEAD_NAME;
    public static final ForgeConfigSpec.BooleanValue SHOW_CHAT_NAME;
    public static final ForgeConfigSpec.EnumValue<TabNameMode> TAB_NAME_MODE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("display");
        SHOW_OVERHEAD_NAME = builder.comment("Show a role name over player heads without changing GameProfile.")
                .define("showOverheadRoleName", true);
        SHOW_CHAT_NAME = builder.comment("Decorate server chat sender names with role names.")
                .define("showChatRoleName", true);
        TAB_NAME_MODE = builder.comment("VANILLA, ROLE or HIDDEN.")
                .defineEnum("tabNameMode", TabNameMode.ROLE);
        builder.pop();
        SPEC = builder.build();
    }

    private RoleCardConfig() {}
}
