package boat.carpetorgaddition.wheel.inventory;

import boat.carpetorgaddition.wheel.Counter;
import boat.carpetorgaddition.wheel.ItemStackCounter;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;

/**
 * 不可变的物品栏，一旦创建，里面的内容都是不可以改变的，只能进行查询操作，否则抛出{@link UnsupportedOperationException}
 */
@NullMarked
public final class ImmutableInventory implements Container, Iterable<ItemStack> {
    /**
     * 空物品栏
     */
    public static final ImmutableInventory EMPTY = new ImmutableInventory();

    private final SimpleContainer inventory;

    public ImmutableInventory(List<ItemStack> list) {
        this.inventory = new SimpleContainer(list.toArray(ItemStack[]::new));
    }

    @SuppressWarnings("unused")
    public ImmutableInventory(Container inventory) {
        ArrayList<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            list.add(inventory.getItem(i));
        }
        this.inventory = new SimpleContainer(list.toArray(ItemStack[]::new));
    }

    public ImmutableInventory(ItemStack itemStack) {
        this(List.of(itemStack));
    }

    private ImmutableInventory() {
        this.inventory = new SimpleContainer();
    }

    @Override
    public int getContainerSize() {
        return this.inventory.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return this == EMPTY || this.inventory.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.inventory.getItem(slot);
    }

    @Deprecated(forRemoval = true)
    @Override
    public ItemStack removeItem(int slot, int amount) {
        throw new UnsupportedOperationException();
    }

    @Deprecated(forRemoval = true)
    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        throw new UnsupportedOperationException();
    }

    @Deprecated(forRemoval = true)
    @Override
    public void setItem(int slot, ItemStack stack) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setChanged() {
        this.inventory.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.inventory.stillValid(player);
    }

    @Deprecated(forRemoval = true)
    @Override
    public void clearContent() {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    public Counter<ItemStack> statistics() {
        ItemStackCounter counter = new ItemStackCounter(true);
        this.inventory.forEach(counter::add);
        return counter;
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", this.getClass().getSimpleName() + ":{", "}");
        for (int index = 0; index < this.getContainerSize(); index++) {
            ItemStack itemStack = this.getItem(index);
            if (itemStack.isEmpty()) {
                continue;
            }
            joiner.add(itemStack.getItem() + "*" + itemStack.getCount());
        }
        return joiner.toString();
    }

    @Override
    public Iterator<ItemStack> iterator() {
        return new Iterator<>() {
            private int index = 0;

            private final int size = ImmutableInventory.this.getContainerSize();

            @Override
            public boolean hasNext() {
                return this.index < this.size;
            }

            @Override
            public ItemStack next() {
                ItemStack itemStack = ImmutableInventory.this.getItem(index);
                this.index++;
                return itemStack;
            }
        };
    }
}
