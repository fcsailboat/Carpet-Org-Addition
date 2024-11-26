package org.carpetorgaddition.util.inventory;

import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;

/**
 * 不可变的物品栏，一旦创建，里面的内容都是不可以改变的，只能进行查询操作，否则抛出{@link UnsupportedOperationException}
 */
public final class ImmutableInventory extends SimpleInventory implements Iterable<ItemStack> {
    /**
     * 当前物品栏是否已锁定，锁定后，物品栏不能改变
     */
    private boolean lock = false;
    /**
     * 空物品栏
     */
    public static final ImmutableInventory EMPTY = new ImmutableInventory(DefaultedList.copyOf(ItemStack.EMPTY));

    public ImmutableInventory(List<ItemStack> list) {
        super(list.size());
        for (int i = 0; i < list.size(); i++) {
            // 不能用super.setStack(i, list.get(i))，编译器会自动把super设置为this
            this.setStack(i, list.get(i));
        }
        this.lock = true;
    }

    public ImmutableInventory(Inventory inventory) {
        this(asList(inventory));
    }

    private static ArrayList<ItemStack> asList(Inventory inventory) {
        ArrayList<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < inventory.size(); i++) {
            list.add(inventory.getStack(i));
        }
        return list;
    }

    @Override
    public boolean isEmpty() {
        return this == EMPTY || super.isEmpty();
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ItemStack removeStack(int slot) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (this.lock) {
            throw new UnsupportedOperationException();
        } else {
            super.setStack(slot, stack);
        }
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ItemStack> clearToList() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ItemStack removeItem(Item item, int count) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ItemStack addStack(ItemStack stack) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        StringJoiner inventory = new StringJoiner(", ", "{", "}");
        for (int index = 0; index < this.size(); index++) {
            ItemStack itemStack = this.getStack(index);
            if (itemStack.isEmpty()) {
                continue;
            }
            inventory.add(itemStack.getItem().toString() + "*" + itemStack.getCount());
        }
        return inventory.toString();
    }

    @NotNull
    @Override
    public Iterator<ItemStack> iterator() {
        return new Iterator<>() {
            // 要返回的下一个元素的索引
            private int cursor = 0;

            // 迭代器的大小
            private final int size = ImmutableInventory.this.size();

            @Override
            public boolean hasNext() {
                return this.cursor < this.size;
            }

            @Override
            public ItemStack next() {
                // 由于对象不可变，所以是线程安全的，不需要考虑并发修改的问题
                ItemStack itemStack = ImmutableInventory.this.getStack(cursor);
                this.cursor++;
                return itemStack;
            }
        };
    }
}
