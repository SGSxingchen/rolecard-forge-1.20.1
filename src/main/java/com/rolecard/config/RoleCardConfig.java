package com.rolecard.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class RoleCardConfig {
    public enum TabNameMode { VANILLA, ROLE, HIDDEN }
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue SHOW_OVERHEAD_NAME, SHOW_CHAT_NAME, ALLOW_PENDING_DRAFT_EDITS, ALLOW_REJECTED_EDITS;
    public static final ForgeConfigSpec.IntValue MAX_BIOGRAPHY_LENGTH;
    public static final ForgeConfigSpec.EnumValue<TabNameMode> TAB_NAME_MODE;
    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("display");
        SHOW_OVERHEAD_NAME = builder.comment("Show a role name over player heads without changing GameProfile.").define("showOverheadRoleName", true);
        SHOW_CHAT_NAME = builder.comment("Decorate server chat sender names with role names.").define("showChatRoleName", true);
        TAB_NAME_MODE = builder.comment("VANILLA, ROLE or HIDDEN.").defineEnum("tabNameMode", TabNameMode.ROLE); builder.pop();
        builder.push("workflow");
        ALLOW_PENDING_DRAFT_EDITS = builder.comment("Allow players to alter a submitted pending card. False prevents review races.").define("allowDraftEditsWhilePending", false);
        ALLOW_REJECTED_EDITS = builder.comment("Allow returned cards to be changed and submitted again.").define("allowRejectedEdits", true);
        MAX_BIOGRAPHY_LENGTH = builder.comment("Maximum biography characters; packet hard cap remains 1500 for network safety.").defineInRange("maxBiographyLength", 1500, 100, 1500);
        builder.pop(); SPEC = builder.build();
    }
    private RoleCardConfig() {}
}
