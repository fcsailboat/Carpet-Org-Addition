package boat.carpetorgaddition.wheel.inventory;

import boat.carpetorgaddition.util.InventoryUtils;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NullMarked;

import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

/**
 * 自动扩容物品栏
 */
@NullMarked
public class AutoGrowInventory implements Container, Iterable<ItemStack> {
    private SimpleContainer inventory = new SimpleContainer(27);
    private int growCount = 0;

    public AutoGrowInventory() {
    }

    @Override
    public int getContainerSize() {
        return this.inventory.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return this.inventory.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.inventory.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return this.inventory.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return this.inventory.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.inventory.setItem(slot, stack);
    }

    @Override
    public void setChanged() {
        this.inventory.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.inventory.stillValid(player);
    }

    @Override
    public void clearContent() {
        this.inventory.clearContent();
    }

    /**
     * 向物品栏内添加物品并自动扩容
     *
     * @param stack 要添加的物品
     */
    public void addStack(ItemStack stack) {
        ItemStack itemStack = tryAddStack(stack);
        InventoryUtils.assertEmptyStack(itemStack);
    }

    /**
     * @return 总是为 {@link ItemStack#EMPTY}
     */
    private ItemStack tryAddStack(ItemStack stack) {
        // 添加过量堆叠的物品
        while (stack.getCount() > stack.getMaxStackSize()) {
            this.tryAddStack(stack.split(stack.getMaxStackSize()));
        }
        ItemStack itemStack = this.inventory.addItem(stack);
        // 物品栏内容足够容纳物品
        if (itemStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        // 计算新物品栏的大小
        int newSize = this.getContainerSize() + (this.getContainerSize() >>> 1);
        // 创建新物品栏，并拷贝物品
        SimpleContainer inventory = new SimpleContainer(newSize);
        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            inventory.setItem(i, this.inventory.getItem(i));
        }
        // 将当前封装的物品栏替换为新物品栏，并重新添加物品
        this.inventory = inventory;
        this.growCount++;
        return this.tryAddStack(itemStack);
    }

    @Override
    public java.util.Iterator<ItemStack> iterator() {
        return new AutoGrowInventoryIterator();
    }

    private class AutoGrowInventoryIterator implements java.util.Iterator<ItemStack> {
        private int index = 0;
        private final int expectedGrowCount = AutoGrowInventory.this.growCount;

        @Override
        public boolean hasNext() {
            return this.index < AutoGrowInventory.this.getContainerSize();
        }

        @Override
        public ItemStack next() {
            // 不能在遍历时更改物品栏大小
            if (AutoGrowInventory.this.growCount != this.expectedGrowCount) {
                throw new ConcurrentModificationException();
            }
            // 索引超出物品栏范围
            if (this.index >= AutoGrowInventory.this.getContainerSize()) {
                throw new NoSuchElementException();
            }
            ItemStack itemStack = AutoGrowInventory.this.getItem(this.index);
            index++;
            return itemStack;
        }
    }
}
