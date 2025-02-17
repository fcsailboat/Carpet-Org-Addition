package org.carpetorgaddition.mixin.util;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@SuppressWarnings("UnusedReturnValue")
@Mixin(RenderLayer.MultiPhaseParameters.Builder.class)
public interface MultiPhaseParametersBuilderAccessor {
    @Invoker("lineWidth")
    RenderLayer.MultiPhaseParameters.Builder setLineWidth(RenderPhase.LineWidth lineWidth);

    @Invoker("layering")
    RenderLayer.MultiPhaseParameters.Builder setLayering(RenderPhase.Layering layering);

    @Invoker("target")
    RenderLayer.MultiPhaseParameters.Builder setTarget(RenderPhase.Target target);

    @Invoker("build")
    RenderLayer.MultiPhaseParameters invokerBuild(boolean affectsOutline);
}
