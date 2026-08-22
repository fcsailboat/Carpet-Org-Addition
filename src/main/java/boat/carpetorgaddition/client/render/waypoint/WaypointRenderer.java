package boat.carpetorgaddition.client.render.waypoint;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.client.util.ClientMessageUtils;
import boat.carpetorgaddition.client.util.ClientUtils;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import org.jetbrains.annotations.Unmodifiable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WaypointRenderer implements HudElement {
    private final Map<Object, Waypoint> waypoints = new HashMap<>();
    @Nullable
    private Matrix4f projection;
    private static WaypointRenderer INSTANCE;

    static {
        // 断开连接时清除路径点
        ClientPlayConnectionEvents.DISCONNECT.register((_, _) -> destroy());
        // 清除不再需要的渲染器
        ClientTickEvents.START_CLIENT_TICK.register(_ -> getInstance().waypoints.values().removeIf(Waypoint::isStopped));
    }

    private WaypointRenderer() {
    }

    @NonNull
    public static WaypointRenderer getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new WaypointRenderer();
        }
        return INSTANCE;
    }

    private static void destroy() {
        INSTANCE = null;
    }

    public Waypoint addOrUpdate(Waypoint waypoint) {
        Waypoint value = this.waypoints.computeIfAbsent(waypoint.getMapDecorationType(), _ -> waypoint);
        // 重置剩余持续时间
        value.update(waypoint);
        return value;
    }

    public Optional<Waypoint> addOrModify(Waypoint waypoint) {
        Waypoint oldWaypoint = this.waypoints.put(waypoint.getMapDecorationType(), waypoint);
        if (oldWaypoint == null) {
            return Optional.empty();
        }
        this.waypoints.put(new Object(), oldWaypoint);
        return Optional.of(oldWaypoint);
    }

    public void stop(Waypoint waypoint) {
        Waypoint oldWaypoint = this.waypoints.remove(waypoint.getMapDecorationType());
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
    public List<Waypoint> listRenderers(MapDecorationType icon) {
        return this.waypoints.values().stream().filter(waypoint -> waypoint.getMapDecorationType().equals(icon)).toList();
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, @NonNull DeltaTracker deltaTracker) {
        if (this.projection == null) {
            return;
        }
        for (Waypoint waypoint : waypoints.values()) {
            try {
                // 绘制图标
                Matrix4fStack modelView = RenderSystem.getModelViewStack();
                modelView.pushMatrix();
                modelView.mul(ClientUtils.getCameraRenderState().viewRotationMatrix);
                waypoint.render(graphics, modelView, this.projection);
                modelView.popMatrix();
            } catch (RuntimeException e) {
                // 发送错误消息，然后停止渲染
                ClientMessageUtils.sendErrorMessage(LocalizationKeys.Render.WAYPOINT.then("error").translate(), e);
                CarpetOrgAddition.LOGGER.error("An unexpected error occurred while rendering waypoint '{}'", waypoint.getName(), e);
                waypoint.discard();
                waypoint.requestServerToStop();
            }
        }
    }

    public void setProjection(@Nullable Matrix4f projection) {
        this.projection = projection;
    }
}
