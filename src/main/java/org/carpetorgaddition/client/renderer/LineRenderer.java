package org.carpetorgaddition.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class LineRenderer {
    private final Tessellator tessellator = Tessellator.getInstance();
    /**
     * 线的颜色
     */
    @NotNull
    private Color color = new Color(1F, 0.8F, 0.4F, 1F);
    @NotNull
    private Vec3d from;
    @NotNull
    private Vec3d to;

    public LineRenderer(@NotNull Vec3d from, @NotNull Vec3d to) {
        this.from = from;
        this.to = to;
    }

    public void render(MatrixStack matrixStack) {
        matrixStack.push();
        MatrixStack.Entry peek = matrixStack.peek();
        Matrix4f matrix4f = peek.getPositionMatrix();
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();
        // 平移渲染框
        matrixStack.translate(-cameraPos.getX(), -cameraPos.getY(), -cameraPos.getZ());
        BufferBuilder bufferBuilder = this.tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.LINES);
        Vec3d relativize = this.from.relativize(this.to);
        bufferBuilder.vertex(matrix4f, (float) this.from.getX(), (float) this.from.getY(), (float) this.from.getZ())
                .color(this.color.red(), this.color.green(), this.color.blue(), this.color.alpha())
                .normal(peek, (float) relativize.getX(), (float) relativize.getY(), (float) relativize.getZ());
        bufferBuilder.vertex(matrix4f, (float) this.to.getX(), (float) this.to.getY(), (float) this.to.getZ())
                .color(this.color.red(), this.color.green(), this.color.blue(), this.color.alpha())
                .normal(peek, (float) relativize.getX(), (float) relativize.getY(), (float) relativize.getZ());
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.lineWidth(3F);
        //noinspection resource
        RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.lineWidth(1F);
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.disableDepthTest();
        matrixStack.pop();
    }

    public void setFrom(@NotNull Vec3d from) {
        this.from = from;
    }

    public void setTo(@NotNull Vec3d to) {
        this.to = to;
    }

    public void setColor(@NotNull Color color) {
        this.color = color;
    }
}
