package boat.carpetorgaddition.network.s2c;

import boat.carpetorgaddition.network.NetworkPacketRegister;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record LibrarianCommodityCacheInvalidationS2CPacket() implements CustomPacketPayload {
    public static final Type<LibrarianCommodityCacheInvalidationS2CPacket> ID = NetworkPacketRegister.ofType("librarian_commodity_cache_invalidation");
    public static final StreamCodec<RegistryFriendlyByteBuf, LibrarianCommodityCacheInvalidationS2CPacket> CODEC = new StreamCodec<>() {
        @Override
        public void encode(RegistryFriendlyByteBuf output, LibrarianCommodityCacheInvalidationS2CPacket value) {
        }

        @Override
        public LibrarianCommodityCacheInvalidationS2CPacket decode(RegistryFriendlyByteBuf input) {
            return new LibrarianCommodityCacheInvalidationS2CPacket();
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
