package org.carpetorgaddition.client.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BuiltBuffer;

public class ClientRenderUtils {
    public static void drawWithGlobalProgram(BuiltBuffer end) {
        RenderSystem.getQuadVertices().bind();
        RenderSystem.getQuadVertices().upload(end);
        RenderSystem.getQuadVertices().draw(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
    }
}
