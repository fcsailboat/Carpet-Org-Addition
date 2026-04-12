package boat.carpetorgaddition.network.s2c;

import boat.carpetorgaddition.network.NetworkPacketRegister;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NullMarked;

/**
 * 不可用槽位同步数据包
 *
 * @param syncId GUI的同步ID
 * @param from   槽位的起始索引
 * @param to     槽位的结束索引
 */
@NullMarked
public record UnavailableSlotSyncS2CPacket(int syncId, int from, int to) implements CustomPacketPayload {
    public static final Type<UnavailableSlotSyncS2CPacket> ID = NetworkPacketRegister.ofType("unavailable_slot_sync");
    public static final StreamCodec<RegistryFriendlyByteBuf, UnavailableSlotSyncS2CPacket> CODEC = new StreamCodec<>() {
        @Override
        public UnavailableSlotSyncS2CPacket decode(RegistryFriendlyByteBuf buf) {
            int syncId = buf.readInt();
            int from = buf.readInt();
            int to = buf.readInt();
            return new UnavailableSlotSyncS2CPacket(syncId, from, to);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, UnavailableSlotSyncS2CPacket value) {
            buf.writeInt(value.syncId);
            buf.writeInt(value.from);
            buf.writeInt(value.to);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
