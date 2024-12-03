package org.carpetorgaddition.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.screen.ScreenHandler;
import org.carpetorgaddition.client.command.DictionaryCommand;
import org.carpetorgaddition.client.command.HighlightCommand;
import org.carpetorgaddition.client.command.argument.ClientBlockPosArgumentType;
import org.carpetorgaddition.client.logger.ClientLogger;
import org.carpetorgaddition.client.renderer.WorldRendererManager;
import org.carpetorgaddition.client.renderer.beaconbox.BeaconBoxRenderer;
import org.carpetorgaddition.client.renderer.villagerpoi.VillagerPoiRenderer;
import org.carpetorgaddition.client.renderer.waypoint.WaypointRenderer;
import org.carpetorgaddition.client.renderer.waypoint.WaypointRendererType;
import org.carpetorgaddition.debug.client.render.ComparatorLevelRender;
import org.carpetorgaddition.debug.client.render.SoulSandItemCountRender;
import org.carpetorgaddition.network.s2c.*;
import org.carpetorgaddition.util.screen.BackgroundSpriteSyncSlot;
import org.carpetorgaddition.util.screen.UnavailableSlotImplInterface;

import java.util.Optional;

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
        ClientPlayNetworking.registerGlobalReceiver(
                WaypointUpdateS2CPacket.ID,
                (payload, context) -> WorldRendererManager.addOrUpdate(
                        new WaypointRenderer(
                                WaypointRendererType.NAVIGATOR,
                                payload.target(),
                                payload.worldId()
                        )
                )
        );
        // 注册路径点清除数据包
        ClientPlayNetworking.registerGlobalReceiver(
                WaypointClearS2CPacket.ID,
                ((payload, context) -> Optional.ofNullable(
                        WorldRendererManager.getOnlyRenderer(
                                WaypointRenderer.class,
                                renderer -> renderer.getRenderType() == WaypointRendererType.NAVIGATOR)
                ).ifPresent(WaypointRenderer::setFade))
        );
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
        ClientPlayNetworking.registerGlobalReceiver(BeaconBoxUpdateS2CPacket.ID, (payload, context) -> {
            // 清除单个信标渲染
            if (BeaconBoxUpdateS2CPacket.ZERO.equals(payload.box())) {
                WorldRendererManager.remove(BeaconBoxRenderer.class, renderer -> renderer.getBlockPos().equals(payload.blockPos()));
                return;
            }
            // 添加或更新信标范围
            BeaconBoxRenderer beaconBoxRenderer = WorldRendererManager.getOrCreate(
                    BeaconBoxRenderer.class,
                    renderer -> renderer.getBlockPos().equals(payload.blockPos()),
                    () -> new BeaconBoxRenderer(payload.blockPos(), payload.box())
            );
            beaconBoxRenderer.setSizeModifier(payload.box());
        });
        // 村民信息同步数据包
        ClientPlayNetworking.registerGlobalReceiver(VillagerPoiSyncS2CPacket.ID, (payload, context) -> {
            // noinspection all
            if (context.client().world.getEntityById(payload.info().geVillagerId()) instanceof VillagerEntity villager) {
                VillagerPoiSyncS2CPacket.VillagerInfo villagerInfo = payload.info();
                VillagerPoiRenderer render = new VillagerPoiRenderer(
                        villager,
                        villagerInfo.getBedPos(),
                        villagerInfo.getJobSitePos(),
                        villagerInfo.getPotentialJobSite()
                );
                WorldRendererManager.addOrUpdate(render);
            }
        });
        // 记录器更新数据包
        ClientPlayNetworking.registerGlobalReceiver(LoggerUpdateS2CPacket.ID, (packet, context) -> ClientLogger.onPacketReceive(packet));
    }

    /**
     * 注册渲染器
     */
    private static void registerRender() {
        // 注册路径点渲染器
        WorldRenderEvents.AFTER_TRANSLUCENT.register(
                context -> WorldRendererManager
                        .getRenderer(WaypointRenderer.class)
                        .forEach(renderer -> renderer.render(context))
        );
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> {
                    // 信标范围渲染器
                    WorldRendererManager.getRenderer(BeaconBoxRenderer.class).forEach(renderer -> renderer.render(context));
                    // 村民信息渲染器
                    WorldRendererManager.getRenderer(VillagerPoiRenderer.class).forEach(renderer -> renderer.render(context));
                }
        );
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
