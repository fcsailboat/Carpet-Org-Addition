package org.carpetorgaddition.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;

import java.util.Objects;

public class BlockOutlineRender {
    private final Tessellator tessellator = Tessellator.getInstance();
    private final BlockPos blockPos;
    private final VoxelShape voxelShape;

    public BlockOutlineRender(BlockPos blockPos) {
        this.blockPos = blockPos;
        ClientWorld world = Objects.requireNonNull(MinecraftClient.getInstance().world);
        BlockState blockState = world.getBlockState(blockPos);
        this.voxelShape = blockState.getOutlineShape(world, this.blockPos);
    }

    public BlockOutlineRender(BlockPos blockPos, VoxelShape voxelShape) {
        this.blockPos = blockPos;
        this.voxelShape = voxelShape;
    }

    public void render(MatrixStack matrixStack) {
        if (voxelShape.isEmpty()) {
            return;
        }
        BufferBuilder bufferBuilder = this.tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.LINES);
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();
        matrixStack.push();
        matrixStack.translate(-cameraPos.getX(), -cameraPos.getY(), -cameraPos.getZ());
        matrixStack.translate(this.blockPos.getX(), this.blockPos.getY(), this.blockPos.getZ());
        MatrixStack.Entry entry = matrixStack.peek();
        Matrix4f matrix4f = entry.getPositionMatrix();
        voxelShape.forEachEdge((minX, minY, minZ, maxX, maxY, maxZ) -> {
            float k = (float) (maxX - minX);
            float l = (float) (maxY - minY);
            float m = (float) (maxZ - minZ);
            float n = MathHelper.sqrt(k * k + l * l + m * m);
            k /= n;
            l /= n;
            m /= n;
            bufferBuilder.vertex(matrix4f, (float) minX, (float) minY, (float) minZ)
                    .color(0F, 0F, 1F, 1F)
                    .normal(entry, k, l, m);
            bufferBuilder.vertex(matrix4f, (float) maxX, (float) maxY, (float) maxZ)
                    .color(0F, 0F, 1F, 1F)
                    .normal(entry, k, l, m);
        });
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(GameRenderer::getRenderTypeLinesProgram);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.disableDepthTest();
        matrixStack.pop();
    }
}
