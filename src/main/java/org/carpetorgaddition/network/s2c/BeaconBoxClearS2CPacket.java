package org.carpetorgaddition.network.s2c;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import org.carpetorgaddition.network.PacketFactory;

/**
 * 信标渲染框清除数据包
 */
@Deprecated(forRemoval = true)
public record BeaconBoxClearS2CPacket() implements CustomPayload {
    public static final Id<BeaconBoxClearS2CPacket> ID = PacketFactory.createId("beacon_box_clear");
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
