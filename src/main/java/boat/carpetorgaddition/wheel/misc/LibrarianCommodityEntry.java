package boat.carpetorgaddition.wheel.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@NullMarked
public record LibrarianCommodityEntry(BlockPos blockPos, List<Map.Entry<Integer, ItemStack>> offers) {
    public static final LibrarianCommodityEntry EMPTY = new LibrarianCommodityEntry(BlockPos.ZERO, List.of());
    private static final StreamCodec<RegistryFriendlyByteBuf, Map.Entry<Integer, ItemStack>> STREAM_OFFER_CODEC = new StreamCodec<>() {
        @Override
        public void encode(RegistryFriendlyByteBuf output, Map.Entry<Integer, ItemStack> value) {
            output.writeInt(value.getKey());
            boolean empty = value.getValue().isEmpty();
            output.writeBoolean(empty);
            if (!empty) {
                ItemStack.STREAM_CODEC.encode(output, value.getValue());
            }
        }

        @Override
        public Map.Entry<Integer, ItemStack> decode(RegistryFriendlyByteBuf input) {
            int price = input.readInt();
            ItemStack commodity = input.readBoolean() ? ItemStack.EMPTY : ItemStack.STREAM_CODEC.decode(input);
            return Map.entry(price, commodity);
        }
    };
    public static final StreamCodec<RegistryFriendlyByteBuf, LibrarianCommodityEntry> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(RegistryFriendlyByteBuf output, LibrarianCommodityEntry value) {
            output.writeBlockPos(value.blockPos());
            List<Map.Entry<Integer, ItemStack>> offers = value.offers();
            output.writeInt(offers.size());
            for (Map.Entry<Integer, ItemStack> offer : offers) {
                STREAM_OFFER_CODEC.encode(output, offer);
            }
        }

        @Override
        public LibrarianCommodityEntry decode(RegistryFriendlyByteBuf input) {
            BlockPos blockPos = input.readBlockPos();
            int size = input.readInt();
            ArrayList<Map.Entry<Integer, ItemStack>> list = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                list.add(STREAM_OFFER_CODEC.decode(input));
            }
            return new LibrarianCommodityEntry(blockPos, list);
        }
    };
}
