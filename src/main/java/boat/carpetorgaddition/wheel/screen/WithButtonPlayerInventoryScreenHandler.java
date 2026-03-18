package boat.carpetorgaddition.wheel.screen;

import boat.carpetorgaddition.periodic.PlayerComponentCoordinator;
import boat.carpetorgaddition.periodic.fakeplayer.FakePlayerUtils;
import boat.carpetorgaddition.util.InventoryUtils;
import boat.carpetorgaddition.wheel.inventory.WithButtonPlayerInventory;
import boat.carpetorgaddition.wheel.inventory.WithButtonPlayerInventory.ButtonInventory;
import boat.carpetorgaddition.wheel.inventory.WithButtonPlayerInventory.StopButtonInventory;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NullMarked;

import java.util.Map;

@NullMarked
public class WithButtonPlayerInventoryScreenHandler extends ChestMenu implements BackgroundSpriteSyncServer {
    private final WithButtonPlayerInventory inventory;
    private static final Map<Integer, Identifier> BACKGROUND_SPRITE_MAP = Map.of(
            1, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET,
            2, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
            3, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS,
            4, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS,
            7, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD
    );

    public WithButtonPlayerInventoryScreenHandler(int containerId, ServerPlayer interviewee, ServerPlayer visitor) {
        WithButtonPlayerInventory inventory = PlayerComponentCoordinator.getCoordinator(interviewee).getWithButtonPlayerInventory();
        super(MenuType.GENERIC_9x6, containerId, visitor.getInventory(), inventory, 6);
        this.inventory = inventory;
    }

    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput containerInput, Player player) {
        Container container = this.inventory.getSubInventory(slotIndex);
        if (buttonNum == FakePlayerUtils.PICKUP_RIGHT_CLICK && container instanceof StopButtonInventory) {
            this.inventory.sort();
            return;
        }
        if (container instanceof ButtonInventory buttonInventory) {
            buttonInventory.onClickd(buttonInventory == this.inventory.getHotbar() ? slotIndex - 9 : 0, this.inventory.getActionPack());
            return;
        }
        super.clicked(slotIndex, buttonNum, containerInput, player);
    }

    /**
     * @return 当没有物品可以移动时，返回{@link ItemStack#EMPTY}，否则返回原物品
     */
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return quickMoveStack(this, slotIndex);
    }

    public static ItemStack quickMoveStack(AbstractContainerMenu menu, int slotIndex) {
        Slot slot = menu.slots.get(slotIndex);
        ItemStack remaining = slot.getItem().copy();
        if (remaining.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack slotStack = slot.getItem();
        // 如果当前槽位位于GUI的上半部分，将物品移动的玩家物品栏槽位
        if (slotIndex < 54) {
            if (quickMove(menu, slotStack, 54, menu.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // 盔甲物品优先移动到盔甲槽
            // 检查槽位物品是否为空：
            // - 如果向盔甲槽，物品栏和副手的移动均失败，则在下面的语句中再次尝试
            // - 这里的向盔甲槽移动，只会将装备移动到正确对应的装备槽位（例如头盔只允许移动到头盔槽位）
            int ordinal = InventoryUtils.getPlayerArmorOrdinal(slotStack);
            if (ordinal != -1 && (quickMove(menu, slotStack, ordinal + 1) && moveToInventory(menu, slotStack)) && quickMove(menu, slotStack, 7) && slotStack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            // 食物和不死图腾优先移动到副手槽
            boolean foodOrTotem = InventoryUtils.isFoodItem(slotStack) || InventoryUtils.isTotemItem(slotStack);
            if (foodOrTotem && (quickMove(menu, slotStack, 7) && moveToInventory(menu, slotStack) && quickMove(menu, slotStack, 1, 5))) {
                return ItemStack.EMPTY;
            }
            // 其他物品优先移动到物品栏
            // 如果物品栏已满，移动到盔甲槽和副手，向盔甲槽移动时，允许将盔甲移动到不对应的装备槽位
            if (quickMove(menu, slotStack, 18, 54) && quickMove(menu, slotStack, 1, 5) && quickMove(menu, slotStack, 7)) {
                return ItemStack.EMPTY;
            }
        }
        if (slotStack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return remaining;
    }

    private static boolean moveToInventory(AbstractContainerMenu menu, ItemStack itemStack) {
        return quickMove(menu, itemStack, 18, 54);
    }

    private static boolean quickMove(AbstractContainerMenu menu, ItemStack itemStack, int index) {
        return quickMove(menu, itemStack, index, index + 1);
    }

    /**
     * @return 是否无法移动物品
     */
    private static boolean quickMove(AbstractContainerMenu menu, ItemStack itemStack, int start, int end) {
        return quickMove(menu, itemStack, start, end, false);
    }

    /**
     * @return 是否无法移动物品
     */
    private static boolean quickMove(AbstractContainerMenu menu, ItemStack itemStack, int start, int end, boolean reverse) {
        ScopedValue<Boolean> value = AbstractPlayerInventoryScreenHandler.QUICK_MOVING_ITEM;
        return ScopedValue.where(value, true).call(() -> !menu.moveItemStackTo(itemStack, start, end, reverse));
    }

    @Override
    public Map<Integer, Identifier> getBackgroundSprite() {
        return BACKGROUND_SPRITE_MAP;
    }
}
