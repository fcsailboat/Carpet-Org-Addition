package org.carpetorgaddition.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.carpetorgaddition.client.util.ClientRenderUtils;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

/**
 * 立方体模型渲染器
 */
public class BoxRenderer {
    private final Tessellator tessellator = Tessellator.getInstance();
    /**
     * 用于确定模型的大小和位置
     */
    @NotNull
    private Box box;
    /**
     * 立方体面的颜色
     */
    @NotNull
    private Color faceColor = new Color(0F, 0.8F, 0F, 0.25F);
    /**
     * 立方体线的颜色
     */
    @NotNull
    private Color lineColor = new Color(1F, 1F, 1F, 0.3F);
    /**
     * 立方体面是否能透过方块渲染
     */
    private boolean seeThroughFace = false;
    /**
     * 立方体线是否能透过方块渲染
     */
    private boolean seeThroughLine = true;

    public BoxRenderer(@NotNull Box box) {
        this.box = box;
    }

    /**
     * 渲染立方体
     */
    public void render(MatrixStack matrixStack) {
        float minX = (float) this.box.minX;
        float minY = (float) this.box.minY;
        float minZ = (float) this.box.minZ;
        float maxX = (float) this.box.maxX;
        float maxY = (float) this.box.maxY;
        float maxZ = (float) this.box.maxZ;
        matrixStack.push();
        MatrixStack.Entry entry = matrixStack.peek();
        Matrix4f matrix4f = entry.getPositionMatrix();
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        Vec3d cameraPos = camera.getPos();
        // 平移渲染框
        matrixStack.translate(-cameraPos.getX(), -cameraPos.getY(), -cameraPos.getZ());
        // 启用半透明渲染
        RenderSystem.enableBlend();
        // 禁用剔除
        RenderSystem.disableCull();
        //noinspection StatementWithEmptyBody
        if (this.seeThroughFace) {
            // 允许填充框透过方块渲染（不需要执行任何代码，这里默认就是禁用深度测试）
        } else {
            // 阻止填充框透过方块渲染
            RenderSystem.enableDepthTest();
        }
        // 避免面与面之间互相遮挡造成颜色错误
        RenderSystem.depthMask(false);
        // 渲染填充框
        BufferBuilder bufferBuilder = this.tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        this.drawFillBox(bufferBuilder, matrix4f, minX, minY, minZ, maxX, maxY, maxZ);
        ClientRenderUtils.draw(RenderLayer.getDebugTriangleFan(), bufferBuilder.end());
        RenderSystem.depthMask(true);
        if (this.seeThroughLine) {
            // 允许填充框透过方块渲染
            RenderSystem.disableDepthTest();
        } else {
            // 阻止填充框透过方块渲染
            RenderSystem.enableDepthTest();
        }
        // 渲染框线
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
        this.drawLineBox(builder, entry, minX, minY, minZ, maxX, maxY, maxZ);
        // 加粗框线
        RenderSystem.lineWidth(2F);
        ClientRenderUtils.draw(ClientRenderUtils.SEE_THROUGH_LINE, builder.end());
        // 启用剔除
        RenderSystem.enableCull();
        // 禁用半透明渲染
        RenderSystem.disableBlend();
        // 允许透过方块渲染
        RenderSystem.enableDepthTest();
        matrixStack.pop();
    }

    /**
     * 渲染面
     */
    private void drawFillBox(
            BufferBuilder bufferBuilder,
            Matrix4f matrix4f,
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ
    ) {
        float[][] fillBoxVertex = this.getFillBoxVertex(minX, minY, minZ, maxX, maxY, maxZ);
        for (float[] arr : fillBoxVertex) {
            bufferBuilder
                    .vertex(matrix4f, arr[0], arr[1], arr[2])
                    .color(this.faceColor.red(), this.faceColor.green(), this.faceColor.blue(), this.faceColor.alpha());
        }
    }

    /**
     * 渲染棱线
     */
    private void drawLineBox(
            BufferBuilder bufferBuilder,
            MatrixStack.Entry entry,
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ
    ) {
        float[][] lineBoxVertex = this.getLineBoxVertex(minX, minY, minZ, maxX, maxY, maxZ);
        for (float[] arr : lineBoxVertex) {
            bufferBuilder
                    .vertex(entry, arr[0], arr[1], arr[2])
                    .color(this.lineColor.red(), this.lineColor.green(), this.lineColor.blue(), this.lineColor.alpha())
                    .normal(entry, arr[3], arr[4], arr[5]);
        }
    }

    /**
     * 获取立方体所有用于绘制面的顶点
     */
    private float[][] getFillBoxVertex(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return new float[][]{
                // 南面
                {minX, minY, minZ},
                {maxX, minY, minZ},
                {maxX, maxY, minZ},
                {minX, maxY, minZ},
                // 北面
                {minX, minY, maxZ},
                {minX, maxY, maxZ},
                {maxX, maxY, maxZ},
                {maxX, minY, maxZ},
                // 东面
                {minX, minY, minZ},
                {minX, maxY, minZ},
                {minX, maxY, maxZ},
                {minX, minY, maxZ},
                // 西面
                {maxX, minY, minZ},
                {maxX, minY, maxZ},
                {maxX, maxY, maxZ},
                {maxX, maxY, minZ},
                // 底面
                {minX, minY, minZ},
                {maxX, minY, minZ},
                {maxX, minY, maxZ},
                {minX, minY, maxZ},
                // 顶面
                {minX, maxY, minZ},
                {maxX, maxY, minZ},
                {maxX, maxY, maxZ},
                {minX, maxY, maxZ}
        };
    }

    /**
     * 获取立方体所有用于绘制棱线的顶点和法线向量，每个一维数组的前三个元素是顶点，后三个元素是法线向量
     */
    private float[][] getLineBoxVertex(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return new float[][]{
                {minX, minY, minZ, 1.0F, 0.0F, 0.0F},
                {maxX, minY, minZ, 1.0F, 0.0F, 0.0F},
                {minX, minY, minZ, 0.0F, 1.0F, 0.0F},
                {minX, maxY, minZ, 0.0F, 1.0F, 0.0F},
                {minX, minY, minZ, 0.0F, 0.0F, 1.0F},
                {minX, minY, maxZ, 0.0F, 0.0F, 1.0F},
                {maxX, minY, minZ, 0.0F, 1.0F, 0.0F},
                {maxX, maxY, minZ, 0.0F, 1.0F, 0.0F},
                {maxX, maxY, minZ, -1.0F, 0.0F, 0.0F},
                {minX, maxY, minZ, -1.0F, 0.0F, 0.0F},
                {minX, maxY, minZ, 0.0F, 0.0F, 1.0F},
                {minX, maxY, maxZ, 0.0F, 0.0F, 1.0F},
                {minX, maxY, maxZ, 0.0F, -1.0F, 0.0F},
                {minX, minY, maxZ, 0.0F, -1.0F, 0.0F},
                {minX, minY, maxZ, 1.0F, 0.0F, 0.0F},
                {maxX, minY, maxZ, 1.0F, 0.0F, 0.0F},
                {maxX, minY, maxZ, 0.0F, 0.0F, -1.0F},
                {maxX, minY, minZ, 0.0F, 0.0F, -1.0F},
                {minX, maxY, maxZ, 1.0F, 0.0F, 0.0F},
                {maxX, maxY, maxZ, 1.0F, 0.0F, 0.0F},
                {maxX, minY, maxZ, 0.0F, 1.0F, 0.0F},
                {maxX, maxY, maxZ, 0.0F, 1.0F, 0.0F},
                {maxX, maxY, minZ, 0.0F, 0.0F, 1.0F},
                {maxX, maxY, maxZ, 0.0F, 0.0F, 1.0F},
        };
    }

    public @NotNull Box getBox() {
        return box;
    }

    public void setBox(@NotNull Box box) {
        this.box = box;
    }

    public void setFaceColor(float red, float green, float blue, float alpha) {
        this.faceColor = new Color(red, green, blue, alpha);
    }

    public void setLineColor(float red, float green, float blue, float alpha) {
        this.lineColor = new Color(red, green, blue, alpha);
    }

    public void setSeeThroughFace(boolean seeThroughFace) {
        this.seeThroughFace = seeThroughFace;
    }

    public void setSeeThroughLine(boolean seeThroughLine) {
        this.seeThroughLine = seeThroughLine;
    }
}
