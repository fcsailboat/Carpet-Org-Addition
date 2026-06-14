package boat.carpetorgaddition.network.s2c;

import boat.carpetorgaddition.network.NetworkPacketRegister;
import boat.carpetorgaddition.wheel.misc.LibrarianCommodityEntry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record LibrarianCommodityResponseS2CPacket(LibrarianCommodityEntry entry) implements CustomPacketPayload {
    public static final Type<LibrarianCommodityResponseS2CPacket> ID = NetworkPacketRegister.ofType("librarian_commodity_response");
    public static final StreamCodec<RegistryFriendlyByteBuf, LibrarianCommodityResponseS2CPacket> CODEC = new StreamCodec<>() {
        @Override
        public void encode(RegistryFriendlyByteBuf output, LibrarianCommodityResponseS2CPacket value) {
            LibrarianCommodityEntry.STREAM_CODEC.encode(output, value.entry());
        }

        @Override
        public LibrarianCommodityResponseS2CPacket decode(RegistryFriendlyByteBuf input) {
            return new LibrarianCommodityResponseS2CPacket(LibrarianCommodityEntry.STREAM_CODEC.decode(input));
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
