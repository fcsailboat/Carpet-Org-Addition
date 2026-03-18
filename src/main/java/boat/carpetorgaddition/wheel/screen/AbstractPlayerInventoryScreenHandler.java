package boat.carpetorgaddition.wheel.screen;

import boat.carpetorgaddition.mixin.accessor.HorseScreenHandlerAccessor;
import boat.carpetorgaddition.util.MathUtils;
import boat.carpetorgaddition.wheel.DisabledSlot;
import boat.carpetorgaddition.wheel.inventory.AbstractCustomSizeInventory;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.Map;

public abstract class AbstractPlayerInventoryScreenHandler<T extends Container> extends AbstractContainerMenu implements UnavailableSlotSyncInterface, BackgroundSpriteSyncServer {
    /**
     * 正在使用Shift移动物品
     */
    public static final ScopedValue<Boolean> QUICK_MOVING_ITEM = ScopedValue.newInstance();
    /**
     * 容器的背景精灵图
     */
    protected static final Map<Integer, Identifier> BACKGROUND_SPRITE_MAP = Map.of(
            36, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET,
            37, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
            38, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS,
            39, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS,
            40, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD,
            41, HorseScreenHandlerAccessor.getEmptyHorseArmorSlotTexture(),
            42, HorseScreenHandlerAccessor.getEmptySaddleSlotTexture()
    );

    /**
     * 玩家物品栏的大小
     */
    protected static final int SIZE = 43;
    /**
     * 玩家正在操作的物品栏
     */
    protected final T inventory;

    /**
     * @param syncId          GUI的同步ID
     * @param playerInventory 操作GUI的玩家的物品栏
     * @param inventory       玩家正在操作的物品栏
     */
    public AbstractPlayerInventoryScreenHandler(int syncId, Inventory playerInventory, T inventory) {
        super(MenuType.GENERIC_9x6, syncId);
        this.inventory = inventory;
        this.addOfflinePlayerInventorySlot();
        this.addPlayerInventorySlot(playerInventory);
        this.addHotSlot(playerInventory);
        AbstractCustomSizeInventory.PLACEHOLDER.setCount(1);
    }

    /**
     * 添加假玩家物品栏槽位（GUI上半部分）
     */
    private void addOfflinePlayerInventorySlot() {
        int index = 0;
        for (int i = 0; i < 6; ++i) {
            for (int j = 0; j < 9; ++j) {
                // 如果槽位id大于玩家物品栏的大小，添加不可用槽位
                if (index >= SIZE) {
                    // 添加不可用槽位
                    this.addSlot(new DisabledSlot(this.inventory, index, 8 + j * 18, 18 + i * 18));
                } else {
                    // 添加普通槽位
                    this.addSlot(new Slot(this.inventory, getIndex(index), 8 + j * 18, 18 + i * 18));
                }
                index++;
            }
        }
    }

    /**
     * 添加当前玩家物品栏槽位（GUi下版部分）
     */
    private void addPlayerInventorySlot(Inventory inventory) {
        int index = 0;
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(inventory, index + 9, 8 + j * 18, 103 + i * 18 + 36));
                index++;
            }
        }
    }

    /**
     * 添加快捷栏槽位
     */
    private void addHotSlot(Inventory inventory) {
        for (int index = 0; index < 9; ++index) {
            this.addSlot(new Slot(inventory, index, 8 + index * 18, 161 + 36));
        }
    }

    /**
     * 重新排列GUI内物品的顺序
     */
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

    /**
     * 重写按住Shift键移动物品
     *
     * @param player    当前移动物品的玩家
     * @param slotIndex 快速移动的插槽索引
     * @return 当没有物品可以移动时，返回{@link ItemStack#EMPTY}，否则返回原物品
     */
    @Override
    public @NonNull ItemStack quickMoveStack(@NonNull Player player, int slotIndex) {
        ItemStack itemStack = ItemStack.EMPTY;
        // 获取当前点击的槽位对象
        Slot slot = this.slots.get(slotIndex);
        // 检查当前槽位上是否有物品
        if (slot.hasItem()) {
            // 获取当前槽位上的物品堆栈对象
            ItemStack slotItemStack = slot.getItem();
            itemStack = slotItemStack.copy();
            // 如果当前槽位位于GUI的上半部分，将物品移动的玩家物品栏槽位
            if (slotIndex < 54) {
                if (this.quickMove(slotItemStack, 54, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 否则，将物品从玩家物品栏移动到玩家物品栏
                if (this.quickMove(slotItemStack, 0, 41, false)) {
                    return ItemStack.EMPTY;
                }
            }
            // 如果当前槽位上的物品为空（物品已经移动），将当前槽位的物品设置为EMPTY
            if (slotItemStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemStack;
    }

    /**
     * @return 是否有物品移动了
     */
    private boolean quickMove(ItemStack stack, int startIndex, int endIndex, boolean fromLast) {
        return ScopedValue.where(QUICK_MOVING_ITEM, true).call(() -> !this.moveItemStackTo(stack, startIndex, endIndex, fromLast));
    }

    @Override
    public int from() {
        return 43;
    }

    @Override
    public int to() {
        return 53;
    }

    @Override
    public Map<Integer, Identifier> getBackgroundSprite() {
        return BACKGROUND_SPRITE_MAP;
    }

    @Override
    public void clicked(int slotIndex, int button, @NonNull ContainerInput input, @NonNull Player player) {
        if (MathUtils.isInRange(this.from(), this.to(), slotIndex)) {
            return;
        }
        super.clicked(slotIndex, button, input, player);
    }

    @Override
    public void removed(@NonNull Player player) {
        super.removed(player);
        AbstractCustomSizeInventory.PLACEHOLDER.setCount(1);
    }
}
