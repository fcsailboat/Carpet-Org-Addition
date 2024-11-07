package org.carpetorgaddition.network.s2c;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.carpetorgaddition.CarpetOrgAddition;

/**
 * 信标渲染框清除数据包
 */
public record BeaconBoxClearS2CPacket() implements CustomPayload {
    private static final Identifier BEACON_BOX_CLEAR = Identifier.of(CarpetOrgAddition.MOD_ID, "beacon_box_clear");
    public static final Id<BeaconBoxClearS2CPacket> ID = new Id<>(BEACON_BOX_CLEAR);
    public static PacketCodec<RegistryByteBuf, BeaconBoxClearS2CPacket> CODEC = new PacketCodec<>() {
        @Override
        public BeaconBoxClearS2CPacket decode(RegistryByteBuf buf) {
            return new BeaconBoxClearS2CPacket();
        }

        @Override
        public void encode(RegistryByteBuf buf, BeaconBoxClearS2CPacket value) {
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
