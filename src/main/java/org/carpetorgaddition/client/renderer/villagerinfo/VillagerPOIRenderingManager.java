package org.carpetorgaddition.client.renderer.villagerinfo;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;

import java.util.HashSet;

public class VillagerPOIRenderingManager {
    public static final HashSet<VillagerPOIRender> VILLAGER_INFO_RENDERS = new HashSet<>();

    public static void register() {
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> frame(context.matrixStack(), context.tickCounter()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> VILLAGER_INFO_RENDERS.clear());
    }

    public static void frame(MatrixStack matrixStack, RenderTickCounter tickCounter) {
        VILLAGER_INFO_RENDERS.removeIf(VillagerPOIRender::shouldStop);
        VILLAGER_INFO_RENDERS.forEach(render -> render.render(matrixStack, tickCounter));
    }
}
