package boat.carpetorgaddition.network.s2c;

import boat.carpetorgaddition.network.NetworkPacketRegister;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record WithButtonScreenSyncS2CPacket(int syncId) implements CustomPacketPayload {
    public static final Type<WithButtonScreenSyncS2CPacket> ID = NetworkPacketRegister.ofType("with_button_screen_sync");
    public static final StreamCodec<RegistryFriendlyByteBuf, WithButtonScreenSyncS2CPacket> CODEC = new StreamCodec<>() {
        @Override
        public WithButtonScreenSyncS2CPacket decode(RegistryFriendlyByteBuf input) {
            int id = input.readInt();
            return new WithButtonScreenSyncS2CPacket(id);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf output, WithButtonScreenSyncS2CPacket value) {
            output.writeInt(value.syncId);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
