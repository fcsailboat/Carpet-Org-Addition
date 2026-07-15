package boat.carpetorgaddition.client.render.waypoint;

import boat.carpetorgaddition.client.util.ClientRenderUtils;
import boat.carpetorgaddition.client.util.ClientUtils;
import boat.carpetorgaddition.util.MathUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.floats.Float2FloatMap;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public abstract class Waypoint {
    /**
     * 路径点图标
     */
    private final TextureAtlasSprite sprite;
    private final MapDecorationType icon;
    /**
     * 路径点已经显示的时间
     */
    private long age;
    /**
     * 路径点剩余持续时间
     */
    private long remaining;
    private Vec3 target;
    @NonNull
    protected Vec3 lastTarget;
    /**
     * 路径点所在时间的注册表项
     */
    @NonNull
    protected ResourceKey<Level> registryKey;
    /**
     * 该路径点是否永久显示
     */
    private final boolean persistent;
    protected float tickDelta = 1F;
    protected float lastTickDelta = 1F;
    /**
     * 路径点消失时间
     */
    private static final long VANISHING_TIME = 4L;
    public static final MapDecorationType HIGHLIGHT = MapDecorationTypes.RED_X.value();
    public static final MapDecorationType NAVIGATOR = MapDecorationTypes.TARGET_X.value();

    public Waypoint(@NonNull ResourceKey<Level> registryKey, @NonNull Vec3 target, MapDecorationType icon, long duration, boolean persistent) {
        this.registryKey = registryKey;
        this.target = target;
        this.lastTarget = target;
        this.icon = icon;
        this.remaining = duration;
        this.persistent = persistent;
        TextureAtlas atlas = ClientUtils.getClient().getAtlasManager().getAtlasOrThrow(AtlasIds.MAP_DECORATIONS);
        this.sprite = atlas.getSprite(icon.assetId());
    }

    public Waypoint(Level world, Vec3 target, MapDecorationType icon, long duration, boolean persistent) {
        this(world.dimension(), target, icon, duration, persistent);
    }

    public void render(GuiGraphicsExtractor graphics, Matrix4fc modelView, Matrix4f projection) {
        if (this.isStopped()) {
            return;
        }
        Vec3 revised = this.getRevisedPos();
        if (revised == null) {
            return;
        }
        float tickDelta = ClientUtils.getTickCounter().getGameTimeDeltaPartialTick(false);
        if (this.tickDelta > tickDelta) {
            this.tick();
        }
        this.lastTickDelta = this.tickDelta;
        this.tickDelta = tickDelta;
        Float2FloatMap.Entry entry = ClientRenderUtils.worldToScreenPoint(revised, modelView, projection);
        if (entry == null) {
            return;
        }
        int width = ClientUtils.getWindowWidth();
        int height = ClientUtils.getWindowHeight();
        graphics.pose().pushMatrix();
        graphics.pose().scale(ClientUtils.getScreenWidth() / (float) width, ClientUtils.getScreenHeight() / (float) height);
        graphics.pose().translate(entry.getFloatKey(), entry.getFloatValue());
        graphics.pose().scale((float) width / ClientUtils.getScreenWidth(), (float) height / ClientUtils.getScreenHeight());
        int widthHeight = 16;
        Camera camera = ClientUtils.getCamera();
        Vec3 offset = revised.subtract(camera.position());
        int renderDistance = ClientUtils.getGameOptions().renderDistance().get() * 16;
        // 修正路径点渲染位置
        Vec3 correction = new Vec3(offset.x(), offset.y(), offset.z());
        if (correction.length() > renderDistance) {
            // 将路径点位置限制在渲染距离内
            correction = correction.normalize().scale(renderDistance);
        }
        double adjustSize = correction.length();
        graphics.pose().scale(this.getScale(adjustSize) * 0.8F);
        graphics.pose().pushMatrix();
        graphics.pose().translate(-widthHeight / 2F, -widthHeight / 2F);
        int alpha = (int) (this.getRenderAlpha() * 255);
        int rgba = MathUtils.rgba(CommonColors.WHITE, alpha);
        int argb = MathUtils.rgbaToArgb(rgba);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.sprite, 0, 0, widthHeight, widthHeight, argb);
        graphics.pose().popMatrix();
        if (this.isWatching(width, height, entry)) {
            renderText(graphics, offset);
        }
        graphics.pose().popMatrix();
    }

    private void renderText(GuiGraphicsExtractor graphics, Vec3 offset) {
        Font textRenderer = ClientUtils.getTextRenderer();
        // 计算距离
        double distance = offset.length();
        String formatted = distance >= 1000 ? "%.1fkm".formatted(distance / 1000) : "%.1fm".formatted(distance);
        TextBuilder builder = TextBuilder.of(formatted);
        // 如果玩家与路径点不在同一纬度，设置距离文本为斜体
        if (!this.registryKey.equals(ClientUtils.getWorld().dimension())) {
            builder.setItalic();
        }
        graphics.pose().pushMatrix();
        Component component = builder.build();
        int width = textRenderer.width(component);
        graphics.pose().translate(0F, 10F);
        graphics.pose().scale(1.2F);
        float backgroundOpacity = ClientUtils.getGameOptions().getBackgroundOpacity(0.25F);
        int opacity = (int) (backgroundOpacity * 255.0F) << 24;
        if (opacity != 0) {
            graphics.fill(-width / 2, 0, width / 2, textRenderer.lineHeight, ARGB.multiply(opacity, CommonColors.WHITE));
        }
        graphics.text(textRenderer, component, -width / 2, 0, CommonColors.WHITE);
        graphics.pose().popMatrix();
    }

    protected float getRenderAlpha() {
        return 1F;
    }

    protected void tick() {
        this.age++;
        if (!this.persistent || this.remaining <= 0) {
            this.remaining--;
        }
    }

    @Nullable
    protected Vec3 getRevisedPos() {
        // 获取玩家所在维度ID
        ResourceKey<Level> key = ClientUtils.getWorld().dimension();
        // 玩家和路径点在同一维度
        Vec3 interpolation = getInterpolation();
        if (this.registryKey.equals(key)) {
            return interpolation;
        }
        Camera camera = ClientUtils.getCamera();
        // 玩家在主世界，路径点在下界，将路径点坐标换算成主世界坐标
        if (ServerUtils.isOverworld(key) && ServerUtils.isTheNether(this.registryKey)) {
            return new Vec3(interpolation.x() * 8, camera.position().y(), interpolation.z() * 8);
        }
        // 玩家在下界，路径点在主世界，将路径点坐标换算成下界坐标
        if (ServerUtils.isTheNether(key) && ServerUtils.isOverworld(this.registryKey)) {
            return new Vec3(interpolation.x() / 8, camera.position().y(), interpolation.z() / 8);
        }
        return null;
    }

    protected Vec3 getInterpolation() {
        return this.target;
    }

    /**
     * 获取路径点大小
     *
     * @param distance 摄像机到路径点的距离，用来抵消远小近大
     */
    private float getScale(double distance) {
        if (this.isStopped()) {
            return 0F;
        }
        // 修正路径点大小，使随着距离的拉远路径点尺寸略微减小
        float scale = Math.max((1F - (((float) distance / 40F) * 0.1F)), 0.75F);
        // 播放出场动画
        if (this.remaining < 0) {
            return this.fade(VANISHING_TIME + (this.remaining - this.tickDelta) + 1, scale);
        }
        // 播放入场动画
        if (this.age < VANISHING_TIME) {
            return this.fade(this.age + this.tickDelta, scale);
        }
        return scale;
    }

    /**
     * 修正正在消失的路径点的大小
     *
     * @param time  剩余消失时间
     * @param scale 路径点的大小
     * @return 路径点的消失动画
     */
    private float fade(float time, float scale) {
        if (time <= 0L) {
            return 0F;
        }
        // 让消失动画先慢后快
        float x = time / VANISHING_TIME;
        // 消失动画（缩放）
        return scale * x * x;
    }

    /**
     * @return 光标是否指向路径点
     */
    private boolean isWatching(int width, int height, Float2FloatMap.Entry entry) {
        double x = width / 2.0 - entry.getFloatKey();
        double y = height / 2.0 - entry.getFloatValue();
        return Math.sqrt(x * x + y * y) < 150.0;
    }

    /**
     * 停止渲染并播放消失动画
     */
    public void stop() {
        if (this.remaining > 0L) {
            this.remaining = 0L;
        }
    }

    /**
     * 停止渲染但不播放消失动画
     */
    public void discard() {
        if (this.isStopped()) {
            return;
        }
        this.remaining = -Integer.MAX_VALUE;
    }

    /**
     * @return 是否已经渲染完成，包括消失动画
     */
    public boolean isStopped() {
        return -(this.remaining - 1) > VANISHING_TIME;
    }

    public MapDecorationType getMapDecorationType() {
        return this.icon;
    }

    public final Vec3 getTarget() {
        return this.target;
    }

    public void setTarget(ResourceKey<Level> registryKey, Vec3 vec3d) {
        this.target = vec3d;
        this.registryKey = registryKey;
    }

    public void update(Waypoint waypoint) {
        this.remaining = waypoint.remaining;
    }

    public long getRemaining() {
        return this.remaining;
    }

    public boolean isPersistent() {
        return this.persistent;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Waypoint that = (Waypoint) o;
        return Objects.equals(sprite, that.sprite) && Objects.equals(registryKey, that.registryKey) && Objects.equals(target, that.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sprite, target);
    }

    public void requestServerToStop() {
    }

    public abstract String getName();
}
