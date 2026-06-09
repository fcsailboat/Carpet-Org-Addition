package boat.carpetorgaddition.network.s2c;

import boat.carpetorgaddition.network.NetworkPacketRegister;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record OldPlayerInventoryScreenSyncS2CPacket(int syncId) implements CustomPacketPayload {
    public static final Type<OldPlayerInventoryScreenSyncS2CPacket> ID = NetworkPacketRegister.ofType("old_player_inventory_screen_sync");
    public static final StreamCodec<RegistryFriendlyByteBuf, OldPlayerInventoryScreenSyncS2CPacket> CODEC = new StreamCodec<>() {
        @Override
        public void encode(RegistryFriendlyByteBuf output, OldPlayerInventoryScreenSyncS2CPacket value) {
            output.writeInt(value.syncId);
        }

        @Override
        public OldPlayerInventoryScreenSyncS2CPacket decode(RegistryFriendlyByteBuf input) {
            return new OldPlayerInventoryScreenSyncS2CPacket(input.readInt());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
