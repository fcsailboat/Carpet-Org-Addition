package org.carpetorgaddition.network.s2c;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.carpetorgaddition.CarpetOrgAddition;

// 村民兴趣点渲染器清除数据包
public record VillagerPOIRenderClearS2CPacket() implements CustomPayload {
    private static final Identifier VILLAGER_POI_RENDER_CLEAR = Identifier.of(CarpetOrgAddition.MOD_ID, "villager_poi_render_clear");
    public static final Id<VillagerPOIRenderClearS2CPacket> ID = new Id<>(VILLAGER_POI_RENDER_CLEAR);
    public static PacketCodec<RegistryByteBuf, VillagerPOIRenderClearS2CPacket> CODEC = new PacketCodec<>() {

        @Override
        public void encode(RegistryByteBuf buf, VillagerPOIRenderClearS2CPacket value) {
        }

        @Override
        public VillagerPOIRenderClearS2CPacket decode(RegistryByteBuf buf) {
            return new VillagerPOIRenderClearS2CPacket();
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
