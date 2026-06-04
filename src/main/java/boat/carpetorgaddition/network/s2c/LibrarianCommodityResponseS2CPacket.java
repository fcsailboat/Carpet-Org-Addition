package boat.carpetorgaddition.network.s2c;

import boat.carpetorgaddition.network.NetworkPacketRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record LibrarianCommodityResponseS2CPacket(BlockPos blockPos, int price, ItemStack commodity) implements CustomPacketPayload {
    public static final Type<LibrarianCommodityResponseS2CPacket> ID = NetworkPacketRegister.ofType("librarian_commodity_response");
    public static final StreamCodec<RegistryFriendlyByteBuf, LibrarianCommodityResponseS2CPacket> CODEC = new StreamCodec<>() {
        @Override
        public void encode(RegistryFriendlyByteBuf output, LibrarianCommodityResponseS2CPacket value) {
            output.writeBlockPos(value.blockPos());
            output.writeInt(value.price());
            boolean empty = value.commodity().isEmpty();
            output.writeBoolean(empty);
            if (!empty) {
                ItemStack.STREAM_CODEC.encode(output, value.commodity);
            }
        }

        @Override
        public LibrarianCommodityResponseS2CPacket decode(RegistryFriendlyByteBuf input) {
            BlockPos blockPos = input.readBlockPos();
            int price = input.readInt();
            ItemStack commodity = input.readBoolean() ? ItemStack.EMPTY : ItemStack.STREAM_CODEC.decode(input);
            return new LibrarianCommodityResponseS2CPacket(blockPos, price, commodity);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
