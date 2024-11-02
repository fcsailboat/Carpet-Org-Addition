package org.carpetorgaddition.event;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.carpetorgaddition.network.s2c.BackgroundSpriteSyncS2CPacket;
import org.carpetorgaddition.network.s2c.UnavailableSlotSyncS2CPacket;
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
    }
}
