package boat.carpetorgaddition.network.s2c;

import boat.carpetorgaddition.network.PacketUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public record PlayerTypeSyncS2CPacket(UUID uuid, boolean fake) implements CustomPacketPayload {
    public static final Type<PlayerTypeSyncS2CPacket> ID = PacketUtils.createId("player_type_sync");
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerTypeSyncS2CPacket> CODEC = new StreamCodec<>() {

        @Override
        public void encode(RegistryFriendlyByteBuf output, PlayerTypeSyncS2CPacket value) {
            output.writeUUID(value.uuid());
            output.writeBoolean(value.fake());
        }

        @Override
        public PlayerTypeSyncS2CPacket decode(RegistryFriendlyByteBuf input) {
            return new PlayerTypeSyncS2CPacket(input.readUUID(), input.readBoolean());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
