package org.carpetorgaddition.network.s2c;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.carpetorgaddition.CarpetOrgAddition;

/**
 * 不可用槽位同步数据包
 *
 * @param syncId GUI的同步ID
 * @param from   槽位的起始索引
 * @param to     槽位的结束索引
 */
public record UnavailableSlotSyncS2CPacket(int syncId, int from, int to) implements CustomPayload {
    private static final Identifier UNAVAILABLE_SLOT_SYNC = Identifier.of(CarpetOrgAddition.MOD_ID, "unavailable_slot_sync");
    public static final Id<UnavailableSlotSyncS2CPacket> ID = new Id<>(UNAVAILABLE_SLOT_SYNC);
    public static PacketCodec<RegistryByteBuf, UnavailableSlotSyncS2CPacket> CODEC = new PacketCodec<>() {
        @Override
        public UnavailableSlotSyncS2CPacket decode(RegistryByteBuf buf) {
            int syncId = buf.readInt();
            int from = buf.readInt();
            int to = buf.readInt();
            return new UnavailableSlotSyncS2CPacket(syncId, from, to);
        }

        @Override
        public void encode(RegistryByteBuf buf, UnavailableSlotSyncS2CPacket value) {
            buf.writeInt(value.syncId);
            buf.writeInt(value.from);
            buf.writeInt(value.to);
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
