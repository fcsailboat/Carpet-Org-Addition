package org.carpetorgaddition.util.inventory;

import com.google.common.collect.ImmutableList;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.collection.DefaultedList;

import java.util.List;

/**
 * 模拟一个玩家物品栏，用来接收读取自NBT的物品
 */
public class SimulatePlayerInventory implements Inventory {
    private final DefaultedList<ItemStack> main = DefaultedList.ofSize(36, ItemStack.EMPTY);
    private final DefaultedList<ItemStack> armor = DefaultedList.ofSize(4, ItemStack.EMPTY);
    private final DefaultedList<ItemStack> offHand = DefaultedList.ofSize(1, ItemStack.EMPTY);
    private final List<DefaultedList<ItemStack>> combinedInventory = ImmutableList.of(this.main, this.armor, this.offHand);

    private SimulatePlayerInventory() {
    }

    /**
     * @see net.minecraft.entity.player.PlayerInventory#readNbt(NbtList)
     */
    public static SimulatePlayerInventory of(NbtCompound nbt, MinecraftServer server) {
        NbtList nbtList = nbt.getList("Inventory", NbtElement.COMPOUND_TYPE);
        SimulatePlayerInventory inventory = new SimulatePlayerInventory();
        for (int i = 0; i < nbtList.size(); i++) {
            NbtCompound nbtCompound = nbtList.getCompound(i);
            int j = nbtCompound.getByte("Slot") & 255;
            ItemStack itemStack = ItemStack.fromNbt(server.getRegistryManager(), nbtCompound).orElse(ItemStack.EMPTY);
            if (j < inventory.main.size()) {
                inventory.main.set(j, itemStack);
            } else if (j >= 100 && j < inventory.armor.size() + 100) {
                inventory.armor.set(j - 100, itemStack);
            } else if (j >= 150 && j < inventory.offHand.size() + 150) {
                inventory.offHand.set(j - 150, itemStack);
            }
        }
        return inventory;
    }

    @Override
    public int size() {
        return this.main.size() + this.armor.size() + this.offHand.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemStack : this.main) {
            if (itemStack.isEmpty()) {
                continue;
            }
            return false;
        }
        for (ItemStack itemStack : this.armor) {
            if (itemStack.isEmpty()) {
                continue;
            }
            return false;
        }
        for (ItemStack itemStack : this.offHand) {
            if (itemStack.isEmpty()) {
                continue;
            }
            return false;
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        List<ItemStack> list = null;
        for (DefaultedList<ItemStack> defaultedList : this.combinedInventory) {
            if (slot < defaultedList.size()) {
                list = defaultedList;
                break;
            }

            slot -= defaultedList.size();
        }
        return list == null ? ItemStack.EMPTY : list.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        List<ItemStack> list = null;
        for (DefaultedList<ItemStack> defaultedList : this.combinedInventory) {
            if (slot < defaultedList.size()) {
                list = defaultedList;
                break;
            }

            slot -= defaultedList.size();
        }
        return list != null && !list.get(slot).isEmpty() ? Inventories.splitStack(list, slot, amount) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStack(int slot) {
        DefaultedList<ItemStack> defaultedList = null;
        for (DefaultedList<ItemStack> defaultedList2 : this.combinedInventory) {
            if (slot < defaultedList2.size()) {
                defaultedList = defaultedList2;
                break;
            }
            slot -= defaultedList2.size();
        }
        if (defaultedList != null && !defaultedList.get(slot).isEmpty()) {
            ItemStack itemStack = defaultedList.get(slot);
            defaultedList.set(slot, ItemStack.EMPTY);
            return itemStack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        DefaultedList<ItemStack> defaultedList = null;
        for (DefaultedList<ItemStack> defaultedList2 : this.combinedInventory) {
            if (slot < defaultedList2.size()) {
                defaultedList = defaultedList2;
                break;
            }
            slot -= defaultedList2.size();
        }
        if (defaultedList != null) {
            defaultedList.set(slot, stack);
        }
    }

    @Override
    public void markDirty() {
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void clear() {
        for (List<ItemStack> list : this.combinedInventory) {
            list.clear();
        }
    }
}
