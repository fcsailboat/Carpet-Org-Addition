package org.carpetorgaddition.network.s2c;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import org.carpetorgaddition.network.PacketFactory;

// 村民兴趣点渲染器清除数据包
@Deprecated(forRemoval = true)
public record VillagerPoiRenderClearS2CPacket() implements CustomPayload {
    public static final Id<VillagerPoiRenderClearS2CPacket> ID = PacketFactory.createId("villager_poi_render_clear");
    public static PacketCodec<RegistryByteBuf, VillagerPoiRenderClearS2CPacket> CODEC = new PacketCodec<>() {

        @Override
        public void encode(RegistryByteBuf buf, VillagerPoiRenderClearS2CPacket value) {
        }

        @Override
        public VillagerPoiRenderClearS2CPacket decode(RegistryByteBuf buf) {
            return new VillagerPoiRenderClearS2CPacket();
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
