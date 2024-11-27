package org.carpetorgaddition.util.inventory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import org.carpetorgaddition.util.InventoryUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 自动扩容物品栏
 */
public class AutoGrowInventory implements Inventory, Iterable<ItemStack> {
    @NotNull
    private SimpleInventory inventory = new SimpleInventory(16);
    private int growCount = 0;

    public AutoGrowInventory() {
    }

    @Override
    public int size() {
        return this.inventory.size();
    }

    @Override
    public boolean isEmpty() {
        return this.inventory.isEmpty();
    }

    @Override
    public ItemStack getStack(int slot) {
        return this.inventory.getStack(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return this.inventory.removeStack(slot, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        return this.inventory.removeStack(slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        this.inventory.setStack(slot, stack);
    }

    @Override
    public void markDirty() {
        this.inventory.markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    @Override
    public void clear() {
        this.inventory.clear();
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
        while (stack.getCount() > stack.getMaxCount()) {
            this.tryAddStack(stack.split(stack.getMaxCount()));
        }
        ItemStack itemStack = this.inventory.addStack(stack);
        // 物品栏内容足够容纳物品
        if (itemStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        // 计算新物品栏的大小
        int newSize = this.size() + (this.size() >>> 1);
        // 创建新物品栏，并拷贝物品
        SimpleInventory inventory = new SimpleInventory(newSize);
        for (int i = 0; i < this.inventory.size(); i++) {
            inventory.setStack(i, this.inventory.getStack(i));
        }
        // 将当前封装的物品栏替换为新物品栏，并重新添加物品
        this.inventory = inventory;
        this.growCount++;
        return this.tryAddStack(itemStack);
    }

    @NotNull
    @Override
    public Iterator<ItemStack> iterator() {
        return new AutoGrowInventoryIterator();
    }

    private class AutoGrowInventoryIterator implements Iterator<ItemStack> {
        private int index = 0;
        private final int expectedGrowCount = AutoGrowInventory.this.growCount;

        @Override
        public boolean hasNext() {
            return this.index < AutoGrowInventory.this.size();
        }

        @Override
        public ItemStack next() {
            // 不能在遍历时更改物品栏大小
            if (AutoGrowInventory.this.growCount != this.expectedGrowCount) {
                throw new ConcurrentModificationException();
            }
            // 索引超出物品栏范围
            if (this.index >= AutoGrowInventory.this.size()) {
                throw new NoSuchElementException();
            }
            ItemStack itemStack = AutoGrowInventory.this.getStack(this.index);
            index++;
            return itemStack;
        }
    }
}
