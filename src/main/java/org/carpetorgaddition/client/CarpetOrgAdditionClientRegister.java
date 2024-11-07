package org.carpetorgaddition.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.screen.ScreenHandler;
import org.carpetorgaddition.client.command.DictionaryCommand;
import org.carpetorgaddition.client.command.HighlightCommand;
import org.carpetorgaddition.client.command.argument.ClientBlockPosArgumentType;
import org.carpetorgaddition.client.renderer.beaconbox.BeaconBoxManager;
import org.carpetorgaddition.client.renderer.waypoint.WaypointRender;
import org.carpetorgaddition.client.renderer.waypoint.WaypointRenderManager;
import org.carpetorgaddition.client.renderer.waypoint.WaypointRenderType;
import org.carpetorgaddition.debug.client.render.ComparatorLevelRender;
import org.carpetorgaddition.debug.client.render.SoulSandItemCountRender;
import org.carpetorgaddition.network.s2c.*;
import org.carpetorgaddition.util.screen.BackgroundSpriteSyncSlot;
import org.carpetorgaddition.util.screen.UnavailableSlotImplInterface;

public class CarpetOrgAdditionClientRegister {
    public static void register() {
        registerCommand();
        registerCommandArgument();
        registerC2SNetworkPack();
        registerNetworkPackReceiver();
        registerRender();
        developed();
    }

    /**
     * 注册客户端命令
     */
    private static void registerCommand() {
        // 高亮路径点命令
        HighlightCommand.register();
        // 字典命令
        DictionaryCommand.register();
    }

    /**
     * 注册客户端命令参数
     */
    private static void registerCommandArgument() {
        // 客户端方块坐标命令参数
        ClientBlockPosArgumentType.register();
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
        ClientPlayNetworking.registerGlobalReceiver(WaypointUpdateS2CPacket.ID, (payload, context) -> WaypointRenderManager.setRender(new WaypointRender(WaypointRenderType.NAVIGATOR, payload.target(), payload.worldId())));
        // 注册路径点清除数据包
        ClientPlayNetworking.registerGlobalReceiver(WaypointClearS2CPacket.ID, ((payload, context) -> WaypointRenderManager.setFade(WaypointRenderType.NAVIGATOR)));
        // 容器不可用槽位同步数据包
        ClientPlayNetworking.registerGlobalReceiver(UnavailableSlotSyncS2CPacket.ID, (payload, context) -> {
            ScreenHandler screen = context.player().currentScreenHandler;
            if (screen.syncId == payload.syncId() && screen instanceof UnavailableSlotImplInterface anInterface) {
                anInterface.sync(payload);
            }
        });
        // 背景精灵同步数据包
        ClientPlayNetworking.registerGlobalReceiver(BackgroundSpriteSyncS2CPacket.ID, (payload, context) -> {
            ScreenHandler screen = context.player().currentScreenHandler;
            if (screen.syncId == payload.syncId() && screen.getSlot(payload.slotIndex()) instanceof BackgroundSpriteSyncSlot slot) {
                slot.setIdentifier(payload.identifier());
            }
        });
        // 信标范围更新数据包
        ClientPlayNetworking.registerGlobalReceiver(BeaconBoxUpdateS2CPacket.ID, (payload, context) -> BeaconBoxManager.setBeaconRender(payload.blockPos(), payload.box()));
        // 信标渲染框清除数据包
        ClientPlayNetworking.registerGlobalReceiver(BeaconBoxClearS2CPacket.ID, (payload, context) -> BeaconBoxManager.clearRender());
    }

    /**
     * 注册渲染器
     */
    private static void registerRender() {
        // 注册路径点渲染器
        WaypointRenderManager.register();
        // 信标范围渲染器
        BeaconBoxManager.register();
    }

    /**
     * 仅用于开发测试
     */
    private static void developed() {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            SoulSandItemCountRender.render();
            ComparatorLevelRender.render();
        }
    }
}
