package org.carpetorgaddition.event;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.carpetorgaddition.logger.LoggerNames;
import org.carpetorgaddition.network.s2c.BackgroundSpriteSyncS2CPacket;
import org.carpetorgaddition.network.s2c.BeaconBoxClearS2CPacket;
import org.carpetorgaddition.network.s2c.UnavailableSlotSyncS2CPacket;
import org.carpetorgaddition.network.s2c.VillagerPOIRenderClearS2CPacket;
import org.carpetorgaddition.util.screen.BackgroundSpriteSyncServer;
import org.carpetorgaddition.util.screen.UnavailableSlotSyncInterface;

public class RegisterEvent {
    public static void register() {
        PlayerOpenHandledScreenEvent.ALREADY_OPENED.register((player, screenHandler) -> {
            // 同步不可用槽位
            if (screenHandler instanceof UnavailableSlotSyncInterface anInterface) {
                ServerPlayNetworking.send(player, new UnavailableSlotSyncS2CPacket(screenHandler.syncId, anInterface.from(), anInterface.to()));
            }
            // 同步槽位背景纹理
            if (screenHandler instanceof BackgroundSpriteSyncServer anInterface) {
                anInterface.getBackgroundSprite().forEach((index, identifier) -> ServerPlayNetworking.send(
                        player,
                        new BackgroundSpriteSyncS2CPacket(screenHandler.syncId, index, identifier)
                ));
            }
        });
        LoggerSubscribeEvent.UNSUBSCRIBE.register((player, logName) -> {
            // 信标渲染框清除数据包
            if (LoggerNames.BEACON_RANGE.equals(logName)) {
                ServerPlayNetworking.send(player, new BeaconBoxClearS2CPacket());
            }
            // 清除村民兴趣点渲染器
            if (LoggerNames.VILLAGER.equals(logName)) {
                ServerPlayNetworking.send(player, new VillagerPOIRenderClearS2CPacket());
            }
        });
        LoggerSubscribeEvent.SUBSCRIBE.register((player, logName, option) -> {
            // 切换选项时，清除之前的渲染器
            if (LoggerNames.VILLAGER.equals(logName)) {
                ServerPlayNetworking.send(player, new VillagerPOIRenderClearS2CPacket());
            }
        });
    }
}
