package org.carpetorgaddition.util.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.carpetorgaddition.util.MathUtils;
import org.carpetorgaddition.util.inventory.AbstractCustomSizeInventory;
import org.carpetorgaddition.util.inventory.ServerPlayerInventory;
import org.carpetorgaddition.util.wheel.DisabledSlot;

import java.util.Map;

public class PlayerInventoryScreenHandler extends ScreenHandler implements UnavailableSlotSyncInterface, BackgroundSpriteSyncServer {
    private static final Map<Integer, Identifier> BACKGROUND_SPRITE_MAP;

    static {
        BACKGROUND_SPRITE_MAP = Map.of(
                36, PlayerScreenHandler.EMPTY_HELMET_SLOT_TEXTURE,
                37, PlayerScreenHandler.EMPTY_CHESTPLATE_SLOT_TEXTURE,
                38, PlayerScreenHandler.EMPTY_LEGGINGS_SLOT_TEXTURE,
                39, PlayerScreenHandler.EMPTY_BOOTS_SLOT_TEXTURE,
                40, PlayerScreenHandler.EMPTY_OFFHAND_ARMOR_SLOT
        );
    }

    private static final int SIZE = 41;
    private final ServerPlayerEntity player;
    private final ServerPlayerInventory inventory;

    public PlayerInventoryScreenHandler(int syncId, PlayerInventory playerInventory, ServerPlayerEntity player) {
        super(ScreenHandlerType.GENERIC_9X6, syncId);
        this.player = player;
        this.inventory = new ServerPlayerInventory(player);
        this.inventory.onOpen(playerInventory.player);
        // 定义变量记录添加的槽位的索引
        int index = 0;
        // 添加上半部分的槽位
        for (int j = 0; j < 6; ++j) {
            for (int k = 0; k < 9; ++k) {
                // 如果槽位id大于玩家物品栏的大小，添加不可用槽位
                if (index >= SIZE) {
                    // 添加不可用槽位
                    this.addSlot(new DisabledSlot(inventory, index, 8 + k * 18, 18 + j * 18));
                } else {
                    // 添加普通槽位
                    this.addSlot(new Slot(inventory, getIndex(index), 8 + k * 18, 18 + j * 18));
                }
                index++;
            }
        }
        // 将记录槽位索引的变量重置为0，然后添加玩家物品栏槽位
        index = 0;
        for (int j = 0; j < 3; ++j) {
            for (int k = 0; k < 9; ++k) {
                this.addSlot(new Slot(playerInventory, index + 9, 8 + k * 18, 103 + j * 18 + 36));
                index++;
            }
        }
        // 将记录槽位索引的变量重置为0，然后添加快捷栏槽位
        for (index = 0; index < 9; ++index) {
            this.addSlot(new Slot(playerInventory, index, 8 + index * 18, 161 + 36));
        }
    }

    // 重新排列GUI内物品的顺序
    private int getIndex(int index) {
        // 物品栏槽位不变
        if (index < 36) {
            return index;
        }
        // 反转盔甲槽的位置
        if (index < 40) {
            return 36 + (39 - index);
        }
        // 副手槽槽位不变
        return index;
    }

    // 按住Shift键移动物品
    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack itemStack = ItemStack.EMPTY;
        // 获取当前点击的槽位对象
        Slot slot = this.slots.get(slotIndex);
        // 检查当前槽位上是否有物品
        if (slot.hasStack()) {
            // 获取当前槽位上的物品堆栈对象
            ItemStack slotItemStack = slot.getStack();
            itemStack = slotItemStack.copy();
            // 如果当前槽位位于GUI的上半部分，将物品移动的玩家物品栏槽位
            if (slotIndex < 54) {
                if (!this.insertItem(slotItemStack, 54, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 否则，将物品从玩家物品栏移动到玩家物品栏
                if (!this.insertItem(slotItemStack, 0, 41, false)) {
                    return ItemStack.EMPTY;
                }
            }
            // 如果当前槽位上的物品为空（物品已经移动），将当前槽位的物品设置为EMPTY
            if (slotItemStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }
        return itemStack;
    }

    // 是否可以打开GUI，如果为false，打开的GUI会自动关闭
    @Override
    public boolean canUse(PlayerEntity player) {
        // 玩家活着，并且玩家没有被删除
        return !this.player.isDead() && !this.player.isRemoved();
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.inventory.dropExcess(player);
        AbstractCustomSizeInventory.PLACEHOLDER.setCount(1);
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (MathUtils.isInRange(this.from(), this.to(), slotIndex)) {
            return;
        }
        super.onSlotClick(slotIndex, button, actionType, player);
    }

    @Override
    public int from() {
        return 41;
    }

    @Override
    public int to() {
        return 53;
    }

    @Override
    public Map<Integer, Identifier> getBackgroundSprite() {
        return BACKGROUND_SPRITE_MAP;
    }
}
