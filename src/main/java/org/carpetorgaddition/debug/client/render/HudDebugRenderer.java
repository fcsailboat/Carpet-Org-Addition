package org.carpetorgaddition.debug.client.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

@FunctionalInterface
public interface HudDebugRenderer {
    void render(DrawContext context, RenderTickCounter tickCounter);
}
