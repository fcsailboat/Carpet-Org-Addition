package org.carpetorgaddition.util.inventory;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;
import org.carpetorgaddition.util.TextUtils;

public abstract class AbstractCustomSizeInventory implements Inventory {
    /**
     * 用来占位的物品
     */
    public static final ItemStack PLACEHOLDER;

    static {
        ItemStack itemStack = new ItemStack(Items.RED_STAINED_GLASS_PANE);
        itemStack.set(DataComponentTypes.CUSTOM_NAME, TextUtils.setColor(TextUtils.translate("carpet.inventory.item.placeholder"), Formatting.RED));
        PLACEHOLDER = itemStack;
    }

    private final DefaultedList<ItemStack> stacks = DefaultedList.ofSize(this.size() - this.getActualSize(), PLACEHOLDER);

    /**
     * @return 物品栏的实际大小，超出此大小的索引都是在GUI中用来占位的，没有实际用途
     */
    protected abstract int getActualSize();

    /**
     * @return 实际可用的物品栏
     */
    protected abstract Inventory getInventory();

    @Override
    public boolean isEmpty() {
        return this.getInventory().isEmpty();
    }

    @Override
    public ItemStack getStack(int slot) {
        if (slot < this.getActualSize()) {
            return this.getInventory().getStack(slot);
        }
        return stacks.get(slot - this.getActualSize());
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        if (slot < this.getActualSize()) {
            return this.getInventory().removeStack(slot, amount);
        }
        ItemStack itemStack = Inventories.splitStack(this.stacks, getAmendSlotIndex(slot), amount);
        if (!itemStack.isEmpty()) {
            this.markDirty();
        }
        return itemStack;
    }

    @Override
    public ItemStack removeStack(int slot) {
        if (slot < this.getActualSize()) {
            return this.getInventory().removeStack(slot);
        }
        ItemStack itemStack = this.stacks.get(getAmendSlotIndex(slot));
        if (itemStack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        this.stacks.set(getAmendSlotIndex(slot), ItemStack.EMPTY);
        return itemStack;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (slot < this.getActualSize()) {
            this.getInventory().setStack(slot, stack);
            return;
        }
        this.stacks.set(getAmendSlotIndex(slot), stack);
        if (!stack.isEmpty() && stack.getCount() > this.getMaxCountPerStack()) {
            stack.setCount(this.getMaxCountPerStack());
        }
        this.markDirty();
    }

    @Override
    public void markDirty() {
        this.getInventory().markDirty();
    }

    @Override
    public void clear() {
        // 只清空物品栏即可，不需要清空用来占位的stacks
        this.getInventory().clear();
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return slot < this.getActualSize();
    }

    // 丢弃多余的槽位中的物品
    public void dropExcess(PlayerEntity player) {
        for (ItemStack itemStack : stacks) {
            // 不丢弃占位用的物品
            if (itemStack == PLACEHOLDER) {
                continue;
            }
            player.dropItem(itemStack, false, false);
        }
    }

    private int getAmendSlotIndex(int slotIndex) {
        return slotIndex - this.getActualSize();
    }
}
