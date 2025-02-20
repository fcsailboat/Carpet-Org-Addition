package org.carpetorgaddition.client.util;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gl.ShaderProgramLayer;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.mixin.util.MultiPhaseParametersBuilderAccessor;
import org.carpetorgaddition.mixin.util.ShaderProgramLayersAccessor;
import org.joml.Matrix4f;

import java.util.Objects;
import java.util.OptionalDouble;

public class ClientRenderUtils {
    public static final RenderLayer SEE_THROUGH_LINE;

    static {
        RenderLayer.MultiPhaseParameters.Builder builder = RenderLayer.MultiPhaseParameters.builder();
        MultiPhaseParametersBuilderAccessor accessor = (MultiPhaseParametersBuilderAccessor) builder;
        accessor.setLineWidth(new RenderPhase.LineWidth(OptionalDouble.empty()));
        accessor.setLayering(RenderPhase.VIEW_OFFSET_Z_LAYERING);
        accessor.setTarget(RenderPhase.ITEM_ENTITY_TARGET);
        SEE_THROUGH_LINE = RenderLayer.of(
                "see_through_line",
                1536,
                ShaderProgramLayer
                        .create(ShaderProgramLayersAccessor.getRenderTypeLines())
                        .id(Identifier.of(CarpetOrgAddition.MOD_ID, "see_through_line"))
                        .depthTest(DepthTestState.NO_DEPTH_TEST)
                        .create(),
                accessor.invokerBuild(false)
        );
    }


    public static void draw(RenderLayer renderLayer, BuiltBuffer builtBuffer) {
        renderLayer.draw(builtBuffer);
    }

    public static void drawWaypoint(Identifier identifier, WorldRenderContext context) {
        RenderLayer renderLayer = RenderLayer.getGuiTexturedOverlay(identifier);
        Matrix4f matrix4f = Objects.requireNonNull(context.matrixStack()).peek().getPositionMatrix();
        VertexConsumer vertexConsumer = Objects.requireNonNull(context.consumers()).getBuffer(renderLayer);
        vertexConsumer.vertex(matrix4f, -1F, -1F, 0F).texture(0F, 0F).color(-1);
        vertexConsumer.vertex(matrix4f, -1F, 1F, 0F).texture(0F, 1F).color(-1);
        vertexConsumer.vertex(matrix4f, 1F, 1F, 0F).texture(1F, 1F).color(-1);
        vertexConsumer.vertex(matrix4f, 1F, -1F, 0F).texture(1F, 0F).color(-1);
    }
}
