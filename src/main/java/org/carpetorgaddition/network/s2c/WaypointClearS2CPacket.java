package org.carpetorgaddition.network.s2c;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import org.carpetorgaddition.network.PacketFactory;
import org.carpetorgaddition.util.GameUtils;

/**
 * 导航点清除数据包
 */
public record WaypointClearS2CPacket() implements CustomPayload {
    public static final CustomPayload.Id<WaypointClearS2CPacket> ID = PacketFactory.createId("waypoint_clear");
    public static PacketCodec<RegistryByteBuf, WaypointClearS2CPacket> CODEC
            = PacketCodec.of((buf, value) -> GameUtils.pass(), buf -> new WaypointClearS2CPacket());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
