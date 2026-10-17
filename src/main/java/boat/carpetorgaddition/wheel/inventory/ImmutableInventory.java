package boat.carpetorgaddition.wheel.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NullMarked;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringJoiner;

/**
 * 不可变的物品栏，一旦创建，里面的内容都是不可以改变的，只能进行查询操作，否则抛出{@link UnsupportedOperationException}
 */
@NullMarked
public abstract sealed class ImmutableInventory implements Container permits
        ImmutableInventory.Empty,
        ImmutableInventory.MultiStack,
        ImmutableInventory.SingleStack {
    /**
     * 空物品栏
     */

    public static ImmutableInventory empty() {
        return Empty.EMPTY;
    }

    public static ImmutableInventory of(ItemStack itemStack) {
        return of(itemStack, true);
    }

    public static ImmutableInventory of(ItemStack itemStack, boolean fold) {
        if (itemStack.isEmpty() && fold) {
            return empty();
        }
        return new SingleStack(itemStack);
    }

    public static ImmutableInventory of(List<ItemStack> list) {
        return switch (list.size()) {
            case 0 -> empty();
            case 1 -> of(list.getFirst(), false);
            default -> new MultiStack(list);
        };
    }

    @Deprecated(forRemoval = true)
    @Override
    public final ItemStack removeItem(int slot, int amount) {
        throw new UnsupportedOperationException();
    }

    @Deprecated(forRemoval = true)
    @Override
    public final ItemStack removeItemNoUpdate(int slot) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void setChanged() {
    }

    @Deprecated(forRemoval = true)
    @Override
    public final void setItem(int slot, ItemStack stack) {
        throw new UnsupportedOperationException();
    }

    @Deprecated(forRemoval = true)
    @Override
    public final void clearContent() {
        throw new UnsupportedOperationException();
    }

    private static final class MultiStack extends ImmutableInventory {
        private final ItemStack[] itemStacks;

        private MultiStack(List<ItemStack> list) {
            this.itemStacks = list.toArray(ItemStack[]::new);
        }

        @Override
        public int getContainerSize() {
            return this.itemStacks.length;
        }

        @Override
        public boolean isEmpty() {
            for (ItemStack itemStack : this.itemStacks) {
                if (itemStack.isEmpty()) {
                    continue;
                }
                return false;
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return slot >= 0 && slot < this.itemStacks.length ? this.itemStacks[slot] : ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public String toString() {
            StringJoiner joiner = new StringJoiner(", ", this.getClass().getSimpleName() + ":{", "}");
            for (ItemStack itemStack : this.itemStacks) {
                if (itemStack.isEmpty()) {
                    continue;
                }
                joiner.add(itemStack.getItem() + "*" + itemStack.getCount());
            }
            return joiner.toString();
        }
    }

    private static final class SingleStack extends ImmutableInventory {
        private final ItemStack itemStack;

        private SingleStack(ItemStack itemStack) {
            this.itemStack = itemStack;
        }

        @Override
        public int getContainerSize() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return this.itemStack.isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return slot == 0 ? this.itemStack : ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public String toString() {
            if (this.itemStack.isEmpty()) {
                return "%s:{}".formatted(this.getClass().getSimpleName());
            }
            return "%s:{%s*%s}".formatted(this.getClass().getSimpleName(), this.itemStack.getItem(), this.itemStack.getCount());
        }
    }

    private static final class Empty extends ImmutableInventory {
        private static final Empty EMPTY = new Empty();

        private Empty() {
        }

        @Override
        public int getContainerSize() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return false;
        }

        @Override
        public String toString() {
            return "%s:{}".formatted(this.getClass().getSimpleName());
        }

        @Override
        public Iterator<ItemStack> iterator() {
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public ItemStack next() {
                    throw new NoSuchElementException("The empty inventory column cannot be traversed");
                }
            };
        }
    }
}
