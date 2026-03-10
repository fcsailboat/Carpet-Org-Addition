package boat.carpetorgaddition.network;

import boat.carpetorgaddition.network.c2s.ObjectSearchTaskC2SPacket;
import boat.carpetorgaddition.network.handler.ObjectSearchTaskPacketHandler;
import boat.carpetorgaddition.network.s2c.*;
import boat.carpetorgaddition.util.ServerUtils;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public class NetworkPacketRegister {
    /**
     * 注册数据包
     */
    public static void register() {
        // 更新导航点数据包
        PayloadTypeRegistry.clientboundPlay().register(WaypointUpdateS2CPacket.ID, WaypointUpdateS2CPacket.CODEC);
        // 清除导航点数据包
        PayloadTypeRegistry.clientboundPlay().register(WaypointClearS2CPacket.ID, WaypointClearS2CPacket.CODEC);
        // 容器禁用槽位同步数据包
        PayloadTypeRegistry.clientboundPlay().register(UnavailableSlotSyncS2CPacket.ID, UnavailableSlotSyncS2CPacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(WithButtonScreenSyncS2CPacket.ID, WithButtonScreenSyncS2CPacket.CODEC);
        // 背景精灵同步数据包
        PayloadTypeRegistry.clientboundPlay().register(BackgroundSpriteSyncS2CPacket.ID, BackgroundSpriteSyncS2CPacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(FakePlayerPathfinderS2CPacket.ID,FakePlayerPathfinderS2CPacket.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PlayerTypeSyncS2CPacket.ID, PlayerTypeSyncS2CPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ObjectSearchTaskC2SPacket.ID, ObjectSearchTaskC2SPacket.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ObjectSearchTaskC2SPacket.ID, new ObjectSearchTaskPacketHandler());
    }

    public static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> ofType(String id) {
        Identifier identifier = ServerUtils.ofIdentifier(id);
        return new CustomPacketPayload.Type<>(identifier);
    }
}
