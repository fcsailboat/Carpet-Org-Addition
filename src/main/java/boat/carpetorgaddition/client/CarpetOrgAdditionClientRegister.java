package boat.carpetorgaddition.client;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.client.command.ClientCommandRegister;
import boat.carpetorgaddition.client.logger.ClientLogger;
import boat.carpetorgaddition.client.renderer.waypoint.NavigatorWaypoint;
import boat.carpetorgaddition.client.renderer.waypoint.Waypoint;
import boat.carpetorgaddition.client.renderer.waypoint.WaypointRenderer;
import boat.carpetorgaddition.client.util.ClientUtils;
import boat.carpetorgaddition.debug.client.render.HudDebugRendererRegister;
import boat.carpetorgaddition.network.s2c.*;
import boat.carpetorgaddition.wheel.screen.BackgroundSpriteSyncSlot;
import boat.carpetorgaddition.wheel.screen.UnavailableSlotClientSide;
import boat.carpetorgaddition.wheel.screen.WithButtonScreenClientSide;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class CarpetOrgAdditionClientRegister {
    public static void register() {
        registerCommand();
        registerC2SNetworkPack();
        registerNetworkPackReceiver();
        registerRenderer();
        registerKeyBinding();
        developed();
    }

    /**
     * 注册客户端命令
     */
    private static void registerCommand() {
        ClientCommandRegister.register();
    }

    /**
     * 注册客户端到服务端的数据包
     */
    private static void registerC2SNetworkPack() {
    }

    /**
     * 注册数据包接收器
     */
    private static void registerNetworkPackReceiver() {
        // 注册路径点更新数据包
        ClientPlayNetworking.registerGlobalReceiver(
                WaypointUpdateS2CPacket.ID,
                (payload, _) -> {
                    WaypointRenderer instance = WaypointRenderer.getInstance();
                    Vec3 target = payload.getTarget();
                    ResourceKey<Level> registryKey = payload.getWorldKey();
                    Waypoint waypoint = instance.addOrUpdate(new NavigatorWaypoint(registryKey, target));
                    waypoint.setTarget(registryKey, target);
                }
        );
        // 注册路径点清除数据包
        ClientPlayNetworking.registerGlobalReceiver(
                WaypointClearS2CPacket.ID,
                (_, _) -> {
                    WaypointRenderer instance = WaypointRenderer.getInstance();
                    instance.listRenderers(Waypoint.NAVIGATOR).forEach(instance::stop);
                }
        );
        // 容器不可用槽位同步数据包
        ClientPlayNetworking.registerGlobalReceiver(UnavailableSlotSyncS2CPacket.ID, (payload, context) -> {
            AbstractContainerMenu screen = context.player().containerMenu;
            if (screen.containerId == payload.syncId() && screen instanceof UnavailableSlotClientSide packet) {
                packet.carpet_Org_Addition$sync(payload);
            }
        });
        // 带按钮屏幕同步数据包
        ClientPlayNetworking.registerGlobalReceiver(WithButtonScreenSyncS2CPacket.ID, (payload, context) -> {
            AbstractContainerMenu screen = context.player().containerMenu;
            if (screen.containerId == payload.syncId() && screen instanceof WithButtonScreenClientSide packet) {
                packet.carpet_Org_Addition$setWithButton();
            }
        });
        // 背景精灵同步数据包
        ClientPlayNetworking.registerGlobalReceiver(BackgroundSpriteSyncS2CPacket.ID, (payload, context) -> {
            AbstractContainerMenu screen = context.player().containerMenu;
            if (screen.containerId == payload.syncId() && screen.getSlot(payload.slotIndex()) instanceof BackgroundSpriteSyncSlot slot) {
                slot.carpet_Org_Addition$setIdentifier(payload.identifier());
            }
        });
        // 记录器更新数据包
        ClientPlayNetworking.registerGlobalReceiver(LoggerUpdateS2CPacket.ID, (packet, _) -> ClientLogger.onPacketReceive(packet));
        ClientPlayNetworking.registerGlobalReceiver(PlayerTypeSyncS2CPacket.ID, (packet, _) -> {
            if (packet.fake()) {
                ClientUtils.FAKE_PLAYERS.add(packet.uuid());
            } else {
                ClientUtils.FAKE_PLAYERS.remove(packet.uuid());
            }
        });
    }

    /**
     * 注册渲染器
     */
    private static void registerRenderer() {
        // 注册路径点渲染器
        LevelRenderEvents.BEFORE_GIZMOS.register(context -> WaypointRenderer.getInstance().render(context));
    }

    /**
     * 注册按键绑定
     */
    private static void registerKeyBinding() {
        // 清除高亮路径点
        KeyMappingHelper.registerKeyMapping(CarpetOrgAdditionClient.CLEAR_WAYPOINT);
    }

    /**
     * 仅用于开发测试
     */
    private static void developed() {
        if (CarpetOrgAddition.isDevelopment()) {
            HudDebugRendererRegister.register();
        }
    }
}
