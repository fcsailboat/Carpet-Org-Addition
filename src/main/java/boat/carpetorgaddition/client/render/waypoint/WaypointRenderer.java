package boat.carpetorgaddition.client.render.waypoint;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.client.util.ClientMessageUtils;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WaypointRenderer {
    private final Map<Object, Waypoint> waypoints = new HashMap<>();
    private static WaypointRenderer INSTANCE;

    static {
        // 断开连接时清除路径点
        ClientPlayConnectionEvents.DISCONNECT.register((_, _) -> destroy());
        // 清除不再需要的渲染器
        ClientTickEvents.START_CLIENT_TICK.register(_ -> getInstance().waypoints.values().removeIf(Waypoint::isStopped));
    }

    private WaypointRenderer() {
    }

    @NotNull
    public static WaypointRenderer getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new WaypointRenderer();
        }
        return INSTANCE;
    }

    private static void destroy() {
        INSTANCE = null;
    }

    /**
     * 绘制路径点
     */
    public void render(LevelRenderContext context) {
        for (Waypoint waypoint : waypoints.values()) {
            try {
                // 绘制图标
                waypoint.render(context);
            } catch (RuntimeException e) {
                // 发送错误消息，然后停止渲染
                ClientMessageUtils.sendErrorMessage(LocalizationKeys.Render.WAYPOINT.then("error").translate(), e);
                CarpetOrgAddition.LOGGER.error("An unexpected error occurred while rendering waypoint '{}'", waypoint.getName(), e);
                waypoint.discard();
                waypoint.requestServerToStop();
            }
        }
    }

    public Waypoint addOrUpdate(Waypoint waypoint) {
        Waypoint value = this.waypoints.computeIfAbsent(waypoint.getIcon(), _ -> waypoint);
        // 重置剩余持续时间
        value.update(waypoint);
        return value;
    }

    public Optional<Waypoint> addOrModify(Waypoint waypoint) {
        Waypoint oldWaypoint = this.waypoints.put(waypoint.getIcon(), waypoint);
        if (oldWaypoint == null) {
            return Optional.empty();
        }
        this.waypoints.put(new Object(), oldWaypoint);
        return Optional.of(oldWaypoint);
    }

    public void stop(Waypoint waypoint) {
        Waypoint oldWaypoint = this.waypoints.remove(waypoint.getIcon());
        if (oldWaypoint == null) {
            return;
        }
        this.waypoints.put(new Object(), oldWaypoint);
        oldWaypoint.stop();
    }

    /**
     * 获取所有匹配的渲染器
     */
    @Unmodifiable
    public List<Waypoint> listRenderers(Identifier icon) {
        return this.waypoints.values().stream().filter(waypoint -> waypoint.getIcon().equals(icon)).toList();
    }
}
