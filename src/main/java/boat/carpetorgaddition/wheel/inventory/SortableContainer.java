package boat.carpetorgaddition.wheel.inventory;

import boat.carpetorgaddition.exception.InfiniteLoopException;
import boat.carpetorgaddition.util.InventoryUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public interface SortableContainer extends Container {
    /**
     * 整理物品栏
     */
    default void sort() {
        int size = this.getContainerSize();
        // 记录所有未被锁定的槽位
        IntList list = new IntArrayList(size);
        // 合并相同的物品
        for (int index = 0; index < size; index++) {
            if (this.isValidSlot(index)) {
                list.add(index);
                ItemStack itemStack = this.getItem(index);
                // 物品不可堆叠或堆叠已满
                if (InventoryUtils.isItemStackFull(itemStack)) {
                    continue;
                }
                this.merge(index, itemStack);
            }
        }
        // 整理物品
        sort(list);
    }

    private void sort(IntList list) {
        if (list.isEmpty()) {
            return;
        }
        int start = 0;
        int end = list.size() - 1;
        // 基准物品
        ItemStack pivot = this.getItem(list.getFirst());
        while (start < end) {
            // 程序是否陷入了死循环
            boolean infiniteLoop = true;
            while (end > start && InventoryUtils.compare(pivot, this.getItem(list.getInt(end))) <= 0) {
                end--;
                infiniteLoop = false;
            }
            while (end > start && InventoryUtils.compare(pivot, this.getItem(list.getInt(start))) >= 0) {
                start++;
                infiniteLoop = false;
            }
            if (infiniteLoop) {
                throw new InfiniteLoopException("Trapped in an infinite loop while sorting items");
            }
            this.swap(list.getInt(start), list.getInt(end));
        }
        // 基准物品归位
        this.swap(list.getFirst(), list.getInt(start));
        sort(list.subList(0, start));
        sort(list.subList(start + 1, list.size()));
    }

    default void merge(int index, ItemStack itemStack) {
        for (int i = index + 1; i < this.getContainerSize(); i++) {
            if (this.isValidSlot(i)) {
                ItemStack slotStack = this.getItem(i);
                if (slotStack.isEmpty()) {
                    continue;
                }
                if (InventoryUtils.canMergeTo(itemStack, slotStack)) {
                    InventoryUtils.mergeStack(itemStack, slotStack);
                    if (itemStack.isEmpty()) {
                        return;
                    }
                }
            }
        }
    }

    /**
     * @return 指定索引的物品是否可以被操作
     */
    default boolean isValidSlot(int index) {
        ItemStack itemStack = this.getItem(index);
        if (itemStack.getCount() > itemStack.getMaxStackSize()) {
            return false;
        }
        if (itemStack == AbstractCustomSizeInventory.PLACEHOLDER) {
            return false;
        }
        return !InventoryUtils.isGcaItem(itemStack);
    }

    /**
     * 交换两个索引上的物品
     */
    default void swap(int first, int second) {
        if (first == second) {
            return;
        }
        ItemStack firstStack = this.getItem(first);
        ItemStack secondStack = this.getItem(second);
        this.setItem(first, secondStack);
        this.setItem(second, firstStack);
    }
}
