package boat.carpetorgaddition.wheel.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

public record LibrarianCommodityEntry(BlockPos blockPos, int price, ItemStack commodity) {
    public static final LibrarianCommodityEntry EMPTY = new LibrarianCommodityEntry(BlockPos.ZERO, -1, ItemStack.EMPTY);
}
