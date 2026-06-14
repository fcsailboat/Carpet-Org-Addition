package boat.carpetorgaddition.network.c2s;

import boat.carpetorgaddition.network.NetworkPacketRegister;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record LibrarianCommodityQueryC2SPacket(GlobalPos globalPos) implements CustomPacketPayload {
    public static final Type<LibrarianCommodityQueryC2SPacket> ID = NetworkPacketRegister.ofType("librarian_commodity_query");
    public static final StreamCodec<RegistryFriendlyByteBuf, LibrarianCommodityQueryC2SPacket> CODEC = new StreamCodec<>() {

        @Override
        public void encode(RegistryFriendlyByteBuf output, LibrarianCommodityQueryC2SPacket value) {
            output.writeGlobalPos(value.globalPos());
        }

        @Override
        public LibrarianCommodityQueryC2SPacket decode(RegistryFriendlyByteBuf input) {
            return new LibrarianCommodityQueryC2SPacket(input.readGlobalPos());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
