package org.carpetorgaddition.client.renderer.waypoint;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.client.util.ClientMessageUtils;
import org.carpetorgaddition.client.util.ClientUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WaypointRenderer {
    private final Map<Object, Waypoint> waypoints = new HashMap<>();
    private final Camera camera;
    private static WaypointRenderer INSTANCE;

    static {
        // 断开连接时清除路径点
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> destroy());
        // 清除不再需要的渲染器
        WorldRenderEvents.START.register(context -> getInstance().waypoints.values().removeIf(Waypoint::isDone));
    }

    private WaypointRenderer() {
        this.camera = ClientUtils.getCamera();
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
    public void render(WorldRenderContext context) {
        MatrixStack matrixStack = context.matrixStack();
        for (Waypoint waypoint : waypoints.values()) {
            try {
                // 允许路径点透过方块渲染
                RenderSystem.disableDepthTest();
                // 绘制图标
                VertexConsumerProvider consumers = context.consumers();
                RenderTickCounter tickCounter = context.tickCounter();
                waypoint.render(matrixStack, consumers, this.camera, tickCounter);
            } catch (RuntimeException e) {
                // 发送错误消息，然后停止渲染
                ClientMessageUtils.sendErrorMessage(e, "carpet.client.render.waypoint.error");
                CarpetOrgAddition.LOGGER.error("An unexpected error occurred while rendering waypoint '{}'", waypoint.getName(), e);
                waypoint.discard();
                waypoint.requestServerToStop();
            }
        }
    }

    public Waypoint addOrUpdate(Waypoint waypoint) {
        return this.waypoints.computeIfAbsent(waypoint.getIcon(), key -> waypoint);
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
