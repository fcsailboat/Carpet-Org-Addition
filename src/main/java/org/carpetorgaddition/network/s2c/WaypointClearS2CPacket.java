package org.carpetorgaddition.network.s2c;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.util.GameUtils;

/**
 * 导航点清除数据包
 */
public record WaypointClearS2CPacket() implements CustomPayload {
    private static final Identifier WAYPOINT_CLEAR = Identifier.of(CarpetOrgAddition.MOD_ID, "waypoint_clear");
    public static final CustomPayload.Id<WaypointClearS2CPacket> ID = new CustomPayload.Id<>(WAYPOINT_CLEAR);
    public static PacketCodec<RegistryByteBuf, WaypointClearS2CPacket> CODEC
            = PacketCodec.of((buf, value) -> GameUtils.pass(), buf -> new WaypointClearS2CPacket());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
