package org.carpetorgaddition.client.renderer.waypoint;

import net.minecraft.util.Identifier;
import org.carpetorgaddition.client.renderer.WorldRendererManager;
import org.carpetorgaddition.client.util.ClientCommandUtils;

import java.util.function.Consumer;

public enum WaypointRendererType {
    /**
     * 高亮
     */
    HIGHLIGHT(Identifier.ofVanilla("textures/map/decorations/red_x.png"), 60000L),
    /**
     * 导航
     */
    NAVIGATOR(Identifier.ofVanilla("textures/map/decorations/target_x.png"), -1L);
    /**
     * 路径点图标
     */
    private final Identifier icon;
    /**
     * 路径点持续时间
     */
    private final long durationTime;
    /**
     * 路径点消失时间
     */
    private static final long VANISHING_TIME = 800L;

    WaypointRendererType(Identifier identifier, long durationTime) {
        this.icon = identifier;
        this.durationTime = durationTime;
    }

    /**
     * @return 获取路径点的图标
     */
    public Identifier getIcon() {
        return this.icon;
    }

    /**
     * 获取路径点大小
     *
     * @param distance  摄像机到路径点的距离，用来抵消远小近大
     * @param startTime 路径点开始渲染的时间
     * @param fade      路径点是否立即消失
     * @param fadeTime  路径点设置立即消失时的时间
     */
    public float getScale(double distance, long startTime, boolean fade, long fadeTime) {
        // 修正路径点大小，使大小不会随着距离的改变而改变
        float scale = (float) distance / 30F;
        // 再次修正路径点大小，使随着距离的拉远路径点尺寸略微减小
        scale = Math.max(scale * (1F - (((float) distance / 40F) * 0.1F)), scale * 0.75F);
        if (fade) {
            long currentTimeMillis = System.currentTimeMillis();
            // 剩余消失时间
            long remainingTime = (fadeTime + VANISHING_TIME) - currentTimeMillis;
            return fade(remainingTime, scale);
        } else if (this.durationTime > 0L) {
            long currentTimeMillis = System.currentTimeMillis();
            long duration = startTime + this.durationTime;
            if (currentTimeMillis < duration) {
                return scale;
            }
            // 剩余消失时间
            long remainingTime = (duration + VANISHING_TIME) - currentTimeMillis;
            return fade(remainingTime, scale);
        } else {
            return scale;
        }
    }

    /**
     * 修正正在消失的路径点的大小
     *
     * @param remainingTime 剩余消失时间
     * @param scale         路径点的大小
     * @return 路径点的消失动画
     */
    private float fade(long remainingTime, float scale) {
        if (remainingTime < 0L) {
            return 0F;
        }
        // 让消失动画先慢后快
        float x = remainingTime / (float) VANISHING_TIME;
        float cubic = x * x * x;
        // 消失动画（缩放）
        return scale * cubic;
    }

    /**
     * 清除高亮路径点
     */
    public void clear() {
        Consumer<WaypointRendererType> consumer = type -> WorldRendererManager.remove(WaypointRenderer.class, renderer -> renderer.getRenderType() == type);
        switch (this) {
            case HIGHLIGHT -> consumer.accept(HIGHLIGHT);
            // 请求服务器停止发送路径点更新数据包
            case NAVIGATOR -> {
                ClientCommandUtils.sendCommand("navigate stop");
                consumer.accept(NAVIGATOR);
            }
        }
    }

    /**
     * @return 获取日志名称
     * @apiNote 不要在游戏中使用
     */
    public String getLogName() {
        String name = switch (this) {
            case HIGHLIGHT -> "高亮";
            case NAVIGATOR -> "导航";
        };
        return "路径点（" + name + "）";
    }
}
