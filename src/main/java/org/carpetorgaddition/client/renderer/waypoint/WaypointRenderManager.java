package org.carpetorgaddition.client.renderer.waypoint;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;

public class WaypointRenderManager {
    private static final EnumMap<WaypointRenderType, WaypointRender> RENDERS = new EnumMap<>(WaypointRenderType.class);

    public static void register() {
        // 注册路径点渲染器
        WorldRenderEvents.LAST.register(WaypointRenderManager::frame);
        // 断开连接时清除路径点
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> WaypointRenderManager.clearAllRender());
    }

    /**
     * 每一帧都调用
     */
    private static void frame(WorldRenderContext context) {
        // 渲染路径点
        RENDERS.forEach((key, value) -> value.drawWaypoint(context));
        // 移除已渲染结束的路径点
        RENDERS.entrySet().removeIf(entry -> entry.getValue().shouldStop());
    }

    /**
     * 设置路径点渲染器
     *
     * @param render 新路径点渲染器
     */
    public static void setRender(WaypointRender render) {
        RENDERS.put(render.getRenderType(), render);
    }

    /**
     * 获取路径点渲染器
     *
     * @param type 渲染器类型
     * @return 路径点渲染器，可能为null
     */
    @Nullable
    public static WaypointRender getRender(WaypointRenderType type) {
        return RENDERS.get(type);
    }

    /**
     * 设置路径点消失
     *
     * @param type 路径点的类型
     */
    public static void setFade(WaypointRenderType type) {
        WaypointRender render = getRender(type);
        if (render != null) {
            render.setFade();
        }
    }

    /**
     * 清除指定渲染器
     *
     * @param type 渲染器类型
     */
    public static void clearRender(WaypointRenderType type) {
        RENDERS.remove(type);
    }

    /**
     * 清除所有渲染器
     */
    public static void clearAllRender() {
        RENDERS.clear();
    }
}
