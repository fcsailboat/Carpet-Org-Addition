package org.carpetorgaddition.client.renderer.waypoint;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.util.Colors;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.client.renderer.WorldRenderer;
import org.carpetorgaddition.client.util.ClientMessageUtils;
import org.carpetorgaddition.client.util.ClientRenderUtils;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.WorldUtils;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class WaypointRenderer implements WorldRenderer {
    private final WaypointRendererType renderType;
    private final Vec3d target;
    private final String worldId;
    private final long startTime = System.currentTimeMillis();
    private boolean fade = false;
    private long fadeTime = -1L;
    private boolean stop = false;

    public WaypointRenderer(WaypointRendererType renderType, Vec3d target, String worldId) {
        this.renderType = renderType;
        this.target = target;
        this.worldId = worldId;
    }

    public WaypointRenderer(WaypointRendererType renderType, Vec3d target, World world) {
        this(renderType, target, WorldUtils.getDimensionId(world));
    }

    /**
     * 绘制路径点
     */
    @Override
    public void render(WorldRenderContext renderContext) {
        MatrixStack matrixStack = renderContext.matrixStack();
        MinecraftClient client = MinecraftClient.getInstance();
        Camera camera = client.gameRenderer.getCamera();
        if (camera == null) {
            return;
        }
        Vec3d vec3d = this.getPos(client);
        if (vec3d == null) {
            return;
        }
        try {
            // 允许路径点透过方块渲染
            RenderSystem.disableDepthTest();
            // 绘制图标
            drawIcon(renderContext, matrixStack, vec3d, camera);
        } catch (RuntimeException e) {
            // 发送错误消息，然后停止渲染
            ClientMessageUtils.sendErrorMessage(e, "carpet.client.render.waypoint.error");
            CarpetOrgAddition.LOGGER.error("渲染{}路径点时遇到意外错误", this.renderType.getLogName(), e);
            this.renderType.clear();
        }
    }

    @Nullable
    private Vec3d getPos(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || this.target == null || this.worldId == null) {
            return null;
        }
        // 获取玩家所在维度ID
        String playerWorldId = WorldUtils.getDimensionId(player.getWorld());
        // 玩家和路径点在同一维度
        if (WorldUtils.equalsWorld(this.worldId, playerWorldId)) {
            return this.target;
        }
        // 玩家在主世界，路径点在下界，将路径点坐标换算成主世界坐标
        if (WorldUtils.isOverworld(playerWorldId) && WorldUtils.isTheNether(this.worldId)) {
            return new Vec3d(this.target.getX() * 8, this.target.getY(), this.target.getZ() * 8);
        }
        // 玩家在下界，路径点在主世界，将路径点坐标换算成下界坐标
        if (WorldUtils.isTheNether(playerWorldId) && WorldUtils.isOverworld(this.worldId)) {
            return new Vec3d(this.target.getX() / 8, this.target.getY(), this.target.getZ() / 8);
        }
        return null;
    }

    /**
     * 绘制路径点图标
     */
    private void drawIcon(WorldRenderContext context, MatrixStack matrixStack, Vec3d target, Camera camera) {
        // 获取摄像机位置
        Vec3d cameraPos = camera.getPos();
        // 玩家距离目标的位置
        Vec3d offset = target.subtract(cameraPos);
        // 获取客户端渲染距离
        int renderDistance = MinecraftClient.getInstance().options.getViewDistance().getValue() * 16;
        // 修正路径点渲染位置
        Vec3d correctionVec3d = new Vec3d(offset.getX(), offset.getY(), offset.getZ());
        if (correctionVec3d.length() > renderDistance) {
            // 将路径点位置限制在渲染距离内
            correctionVec3d = correctionVec3d.normalize().multiply(renderDistance);
        }
        matrixStack.push();
        // 将路径点平移到方块位置
        matrixStack.translate(correctionVec3d.getX(), correctionVec3d.getY(), correctionVec3d.getZ());
        // 固定路径点大小，防止因距离的改变而改变（远小近大）
        float scale = this.renderType.getScale(correctionVec3d.length(), this.startTime, this.fade, this.fadeTime);
        if (scale <= 0F) {
            this.stop = true;
            return;
        }
        matrixStack.scale(scale, scale, scale);
        // 翻转路径点
        matrixStack.multiply(new Quaternionf(-1, 0, 0, 0));
        // 让路径点始终对准玩家
        matrixStack.multiply(new Quaternionf().rotateY((float) ((Math.PI / 180.0) * (camera.getYaw() - 180F))));
        matrixStack.multiply(new Quaternionf().rotateX((float) ((Math.PI / 180.0) * (-camera.getPitch()))));
        MatrixStack.Entry entry = matrixStack.peek();
        Matrix4f matrix4f = entry.getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        // 绘制图标纹理
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        bufferBuilder.vertex(matrix4f, -1F, -1F, 0F).texture(0, 0);
        bufferBuilder.vertex(matrix4f, -1F, 1F, 0F).texture(0, 1);
        bufferBuilder.vertex(matrix4f, 1F, 1F, 0F).texture(1, 1);
        bufferBuilder.vertex(matrix4f, 1F, -1F, 0F).texture(1, 0);
        //noinspection resource
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
        RenderSystem.setShaderTexture(0, renderType.getIcon());
        // 将缓冲区绘制到屏幕上。
        ClientRenderUtils.drawWithGlobalProgram(bufferBuilder.end());
        tessellator.clear();
        // 如果准星正在指向路径点，显示文本
        if (pointerPointing(camera, target)) {
            drawDistance(context, matrixStack, offset, tessellator);
        }
        matrixStack.pop();
    }

    /**
     * 绘制距离文本
     */
    private void drawDistance(WorldRenderContext context, MatrixStack matrixStack, Vec3d offset, Tessellator tessellator) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        // 计算距离
        double distance = offset.length();
        String formatted = distance >= 1000 ? "%.1fkm".formatted(distance / 1000) : "%.1fm".formatted(distance);
        MutableText text = TextUtils.createText(formatted);
        // 如果玩家与路径点不在同一纬度，设置距离文本为斜体
        if (WorldUtils.differentWorld(this.worldId, WorldUtils.getDimensionId(context.world()))) {
            text = TextUtils.toItalic(text);
        }
        // 获取文本宽度
        int width = textRenderer.getWidth(formatted);
        // 获取背景不透明度
        float backgroundOpacity = MinecraftClient.getInstance().options.getTextBackgroundOpacity(0.25F);
        int opacity = (int) (backgroundOpacity * 255.0F) << 24;
        matrixStack.push();
        // 缩小文字
        matrixStack.scale(0.15F, 0.15F, 0.15F);
        // 渲染文字
        textRenderer.draw(text, -width / 2F, 8, Colors.WHITE, false,
                matrixStack.peek().getPositionMatrix(), context.consumers(),
                TextRenderer.TextLayerType.SEE_THROUGH, opacity, 1);
        tessellator.clear();
        matrixStack.pop();
    }

    /**
     * @return 光标是否指向路径点
     * @see EndermanEntity#isPlayerStaring(PlayerEntity)
     */
    @SuppressWarnings("JavadocReference")
    private boolean pointerPointing(Camera camera, Vec3d target) {
        float f = camera.getPitch() * (float) (Math.PI / 180.0);
        float g = -camera.getYaw() * (float) (Math.PI / 180.0);
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        Vec3d vec3d = new Vec3d(i * j, -k, h * j).normalize();
        Vec3d vec3d2 = new Vec3d(target.getX() - camera.getPos().getX(), target.getY() - camera.getPos().getY(), target.getZ() - camera.getPos().getZ());
        double d = vec3d2.length();
        vec3d2 = vec3d2.normalize();
        double e = vec3d.dotProduct(vec3d2);
        return e > 0.999 - (0.025 / d);
    }

    public boolean equalsTarget(WaypointRenderer renderer) {
        return this.target.equals(renderer.target) && WorldUtils.equalsWorld(this.worldId, renderer.worldId);
    }

    public Vec3d getPos() {
        return target;
    }

    public WaypointRendererType getRenderType() {
        return this.renderType;
    }

    /**
     * @return 是否应该停止渲染
     */
    public boolean shouldStop() {
        return this.stop;
    }

    /**
     * 设置该路径点消失
     */
    public void setFade() {
        this.fade = true;
        this.fadeTime = System.currentTimeMillis();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return renderType == ((WaypointRenderer) o).renderType;
    }

    @Override
    public int hashCode() {
        return renderType.hashCode();
    }
}
