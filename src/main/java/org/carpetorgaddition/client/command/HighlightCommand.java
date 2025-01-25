package org.carpetorgaddition.client.command;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.util.math.Vec3d;
import org.carpetorgaddition.client.command.argument.ClientBlockPosArgumentType;
import org.carpetorgaddition.client.renderer.WorldRendererManager;
import org.carpetorgaddition.client.renderer.waypoint.WaypointRenderer;
import org.carpetorgaddition.client.renderer.waypoint.WaypointRendererType;
import org.jetbrains.annotations.Nullable;

public class HighlightCommand {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("highlight")
                        .then(ClientCommandManager.argument("blockPos", ClientBlockPosArgumentType.blockPos())
                                .executes(HighlightCommand::highlight))
                        .then(ClientCommandManager.literal("clear")
                                .executes(context -> clear()))));
    }

    // 高亮路径点
    // TODO 添加持续时间控制
    private static int highlight(CommandContext<FabricClientCommandSource> context) {
        Vec3d vec3d = ClientBlockPosArgumentType.getBlockPos(context, "blockPos").toCenterPos();
        ClientWorld world = context.getSource().getWorld();
        // 获取旧路径点
        WaypointRenderer oldRender = getWaypointRenderer();
        // 创建新路径点
        WaypointRenderer newRender = new WaypointRenderer(WaypointRendererType.HIGHLIGHT, vec3d, world);
        // 如果两个路径点指向同一个位置，就让玩家看向该路径点
        if (oldRender != null && oldRender.equalsTarget(newRender)) {
            // if语句结束后仍要设置新路径点，因为要重置持续时间
            context.getSource().getEntity().lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, vec3d);
        }
        // 设置新的路径点
        WorldRendererManager.addOrUpdate(newRender);
        return 1;
    }

    // 取消高亮路径点
    private static int clear() {
        WaypointRenderer render = getWaypointRenderer();
        if (render == null) {
            return 0;
        }
        render.setFade();
        return 1;
    }

    @Nullable
    private static WaypointRenderer getWaypointRenderer() {
        return WorldRendererManager.getOnlyRenderer(WaypointRenderer.class, renderer -> renderer.getRenderType() == WaypointRendererType.HIGHLIGHT);
    }
}
