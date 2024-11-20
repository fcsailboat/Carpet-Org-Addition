package org.carpetorgaddition.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.carpetorgaddition.network.s2c.*;

public class NetworkS2CPacketRegister {
    /**
     * 注册数据包
     */
    public static void register() {
        // 更新导航点数据包
        PayloadTypeRegistry.playS2C().register(WaypointUpdateS2CPacket.ID, WaypointUpdateS2CPacket.CODEC);
        // 清除导航点数据包
        PayloadTypeRegistry.playS2C().register(WaypointClearS2CPacket.ID, WaypointClearS2CPacket.CODEC);
        // 容器禁用槽位同步数据包
        PayloadTypeRegistry.playS2C().register(UnavailableSlotSyncS2CPacket.ID, UnavailableSlotSyncS2CPacket.CODEC);
        // 背景精灵同步数据包
        PayloadTypeRegistry.playS2C().register(BackgroundSpriteSyncS2CPacket.ID, BackgroundSpriteSyncS2CPacket.CODEC);
        // 信标范围更新数据包
        PayloadTypeRegistry.playS2C().register(BeaconBoxUpdateS2CPacket.ID, BeaconBoxUpdateS2CPacket.CODEC);
        // 信标渲染框清除数据包
        PayloadTypeRegistry.playS2C().register(BeaconBoxClearS2CPacket.ID, BeaconBoxClearS2CPacket.CODEC);
        // 村民信息同步数据包
        PayloadTypeRegistry.playS2C().register(VillagerPoiSyncS2CPacket.ID, VillagerPoiSyncS2CPacket.CODEC);
        // 村民信息渲染器清除数据包
        PayloadTypeRegistry.playS2C().register(VillagerPoiRenderClearS2CPacket.ID, VillagerPoiRenderClearS2CPacket.CODEC);
    }
}
