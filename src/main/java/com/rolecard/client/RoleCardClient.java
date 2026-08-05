package com.rolecard.client;

import com.rolecard.RoleCardMod;
import com.rolecard.network.ClientHooks;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = RoleCardMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class RoleCardClient {
    private static final KeyMapping OPEN_KEY = new KeyMapping("key.rolecard.open", GLFW.GLFW_KEY_I, "key.categories.rolecard");
    @SubscribeEvent public static void registerKey(RegisterKeyMappingsEvent event) { event.register(OPEN_KEY); ClientHooks.install(ClientCardCache::update); ClientHooks.installPublicName(ClientDisplayNames::update); ClientHooks.installAdmin((id, name, card) -> Minecraft.getInstance().setScreen(new AdminRoleCardScreen(id, name, card))); ClientHooks.installFeedback((message, error) -> { if (Minecraft.getInstance().screen instanceof AdminRoleCardScreen screen) screen.receiveServerFeedback(message, error); }); }

    @Mod.EventBusSubscriber(modid = RoleCardMod.MOD_ID, value = Dist.CLIENT)
    public static final class ForgeEvents {
        @SubscribeEvent public static void clientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END && OPEN_KEY.consumeClick() && Minecraft.getInstance().player != null) Minecraft.getInstance().setScreen(new RoleCardScreen());
        }
        @SubscribeEvent public static void renderName(RenderNameTagEvent event) {
            if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
                String name = ClientDisplayNames.get(player.getUUID());
                if (name != null) event.setContent(net.minecraft.network.chat.Component.literal(name));
            }
        }
    }
    private RoleCardClient() {}
}
