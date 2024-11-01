package org.carpetorgaddition.util.screen;

import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.carpetorgaddition.util.inventory.AbstractCustomSizeInventory;
import org.carpetorgaddition.util.inventory.VillagerInventory;
import org.carpetorgaddition.util.wheel.DisabledSlot;

public class VillagerScreenHandler extends ScreenHandler implements UnavailableSlotSyncInterface{
    // 物品栏的大小
    private static final int SIZE = 8;
    private final VillagerEntity villagerEntity;
    private final VillagerInventory inventory;

    public VillagerScreenHandler(int syncId, PlayerInventory playerInventory, VillagerEntity villagerEntity) {
        super(ScreenHandlerType.GENERIC_3X3, syncId);
        this.villagerEntity = villagerEntity;
        this.inventory = new VillagerInventory(villagerEntity);
        this.inventory.onOpen(playerInventory.player);
        // 添加村民物品栏的槽位
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                if (i == 2 && j == 2) {
                    // 添加不可用的槽位
                    this.addSlot(new DisabledSlot(inventory, j + i * 3, 62 + j * 18, 17 + i * 18));
                    // 添加普通槽位
                } else {
                    this.addSlot(new Slot(inventory, j + i * 3, 62 + j * 18, 17 + i * 18));
                }
            }
        }
        // 添加玩家非快捷栏的物品栏槽位
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        // 添加快捷栏槽位
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    // 快速移动物品的方法，代码直接从其他类中复制过来再改一下
    // 虽然这里对快速移动的方法进行了重写，但是在客户端仍然会调用Generic3x3ContainerScreenHandler类中的快速移动方法（需要验证）
    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        // 创建一个空物品堆栈对象
        ItemStack itemStack = ItemStack.EMPTY;
        // 获取当前GUI中指定索引的槽位对象
        Slot slot = this.slots.get(slotIndex);
        // 判断这个槽位上是否有物品
        if (slot.hasStack()) {
            // 获取这个槽位上的物品堆栈对象
            ItemStack slotItemStack = slot.getStack();
            // 将当前槽位上物品堆栈对象的副本赋值给空物品对象
            itemStack = slotItemStack.copy();
            // 判断当前点击的槽位是否是上方GUI的槽位而不是下方玩家物品栏的槽位
            // insertItem()方法的返回值是物品堆栈的堆叠数是否减少了
            if (slotIndex <= SIZE) {
                // 如果是GUI上半部分的槽位，将上方的物品移动到玩家物品栏的槽位中
                if (!this.insertItem(slotItemStack, 9, 45, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 如果是GUI下半部分的槽位，将玩家物品栏中的物品移动到上方槽位
                if (!this.insertItem(slotItemStack, 0, 8, false)) {
                    // 如果无法移动，检测槽位是不是非快捷栏的物品栏
                    if (slotIndex <= 35) {
                        // 如果物品栏槽位不是快捷栏，将物品移动到快捷栏
                        if (!this.insertItem(slotItemStack, 36, 45, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else {
                        // 如果是快捷栏，将物品移动到物品栏
                        if (!this.insertItem(slotItemStack, 9, 36, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
                // 如果当前槽位上的物品为空（不一定是EMPTY，也可能是空气，或者堆叠数<=0），就设置物品槽位改槽位上的物品为EMPTY
                if (slotItemStack.isEmpty()) {
                    slot.setStack(ItemStack.EMPTY);
                } else {
                    // 没有读懂markDirty()方法的含义
                    slot.markDirty();
                }
                // 如果当前槽位的堆叠数与移动物品前创建的物品堆栈副本的堆叠数相同，就返回空物品堆栈
                // 但是为什么要有这次判断没有看懂，不过也懒得深入研究了
                if (slotItemStack.getCount() == itemStack.getCount()) {
                    return ItemStack.EMPTY;
                }
                slot.onTakeItem(player, slotItemStack);
            }
        }
        // 方法的返回值是干什么用的？
        return itemStack;
    }

    // 村民死亡或距离玩家过远时，自动关闭GUI
    @Override
    public boolean canUse(PlayerEntity player) {
        if (villagerEntity.isDead() || villagerEntity.isRemoved()) {
            return false;
        }
        return player.distanceTo(villagerEntity) < 8;
    }

    // 是否可以向槽位中放入物品
    @Override
    public boolean canInsertIntoSlot(Slot slot) {
        // 槽位不能是禁用的槽位
        return !(slot instanceof DisabledSlot);
    }

    // 是否可以向槽位中放入物品
    @Override
    public boolean canInsertIntoSlot(ItemStack stack, Slot slot) {
        return !(slot instanceof DisabledSlot);
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex == 8) {
            return;
        }
        super.onSlotClick(slotIndex, button, actionType, player);
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.inventory.dropExcess(player);
        AbstractCustomSizeInventory.PLACEHOLDER.setCount(1);
    }

    @Override
    public int from() {
        return 8;
    }

    @Override
    public int to() {
        return 8;
    }
}
