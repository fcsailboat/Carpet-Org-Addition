package boat.carpetorgaddition.network.s2c;

import boat.carpetorgaddition.network.NetworkPacketRegister;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record LibrarianCommodityFunctionS2CPacket(boolean enable) implements CustomPacketPayload {
    public static final Type<LibrarianCommodityFunctionS2CPacket> ID = NetworkPacketRegister.ofType("librarian_commodity_function");
    public static final StreamCodec<RegistryFriendlyByteBuf, LibrarianCommodityFunctionS2CPacket> CODEC = new StreamCodec<>() {
        @Override
        public void encode(RegistryFriendlyByteBuf output, LibrarianCommodityFunctionS2CPacket value) {
            output.writeBoolean(value.enable());
        }

        @Override
        public LibrarianCommodityFunctionS2CPacket decode(RegistryFriendlyByteBuf input) {
            return new LibrarianCommodityFunctionS2CPacket(input.readBoolean());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
