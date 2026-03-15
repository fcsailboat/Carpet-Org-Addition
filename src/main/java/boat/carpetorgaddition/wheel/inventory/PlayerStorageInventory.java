package boat.carpetorgaddition.wheel.inventory;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.periodic.fakeplayer.FakePlayerUtils;
import boat.carpetorgaddition.util.InventoryUtils;
import boat.carpetorgaddition.wheel.screen.QuickShulkerScreenHandler;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntImmutableList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.CheckReturnValue;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.function.Predicate;

/**
 * 一个不包括盔甲槽的玩家物品栏
 */
@NullMarked
public class PlayerStorageInventory implements PlayerDecomposedContainer, SortableContainer {
    private final Inventory playerInventory;
    private final ServerPlayer player;
    private final IntList indexMapping;

    public PlayerStorageInventory(ServerPlayer player) {
        this.playerInventory = player.getInventory();
        this.player = player;
        IntArrayList list = new IntArrayList(37);
        NonNullList<ItemStack> main = this.playerInventory.getNonEquipmentItems();
        for (int i = 0; i < main.size(); i++) {
            list.add(i);
        }
        list.add(Inventory.SLOT_OFFHAND);
        this.indexMapping = new IntImmutableList(list);
    }

    @Override
    public int getContainerSize() {
        return this.indexMapping.size();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < this.getContainerSize(); i++) {
            ItemStack itemStack = this.getItem(i);
            if (itemStack.isEmpty()) {
                continue;
            }
            return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.playerInventory.getItem(this.map(slot));
    }

    public ItemStack getStack(InteractionHand hand) {
        return switch (hand) {
            case MAIN_HAND -> this.player.getMainHandItem();
            case OFF_HAND -> this.player.getOffhandItem();
        };
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return this.playerInventory.removeItem(this.map(slot), amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return this.playerInventory.removeItemNoUpdate(this.map(slot));
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.playerInventory.setItem(this.map(slot), stack);
    }

    public void setStack(InteractionHand hand, ItemStack stack) {
        this.player.setItemInHand(hand, stack);
    }

    @Override
    public void setChanged() {
        this.playerInventory.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.playerInventory.stillValid(player);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < this.getContainerSize(); i++) {
            this.setItem(i, ItemStack.EMPTY);
        }
    }

    public void drop(int index) {
        ItemStack itemStack = this.getItem(index);
        this.setItem(index, ItemStack.EMPTY);
        this.player.drop(itemStack, false, true);
    }

    /**
     * 根据条件丢弃物品栏中所有物品
     *
     * @return 是否丢弃了物品
     */
    public boolean drop(Predicate<ItemStack> predicate) {
        boolean dropped = false;
        for (int i = 0; i < this.getContainerSize(); i++) {
            ItemStack itemStack = this.getItem(i);
            if (itemStack.isEmpty()) {
                continue;
            }
            if (predicate.test(itemStack)) {
                this.drop(i);
                dropped = true;
            }
        }
        return dropped;
    }

    /**
     * 向物品栏中插入物品，优先插入到潜影盒物品中
     *
     * @return 物品栏是否满了
     */
    public boolean insertWithShulkerBoxPriority(ItemStack itemStack) {
        ItemStack remaining = insertToShulkerBox(itemStack);
        if (remaining.isEmpty()) {
            return false;
        }
        // 潜影盒没有足够空间，插入到物品栏中
        this.insert(remaining);
        if (remaining.isEmpty()) {
            return false;
        }
        // 潜影盒和物品栏都没有足够空间，丢弃物品
        FakePlayerUtils.dropItem(this.player, remaining);
        return true;
    }

    /**
     * 向物品栏中插入物品，优先插入到物品栏中
     */
    public void insertWithInventoryPriority(ItemStack itemStack) {
        this.insert(itemStack);
        if (itemStack.isEmpty()) {
            return;
        }
        // 物品栏中没有足够的空间，插入到潜影盒中
        ItemStack remaining = this.insertToShulkerBox(itemStack);
        if (remaining.isEmpty()) {
            return;
        }
        // 物品栏和潜影盒都没有足够空间，丢弃物品
        FakePlayerUtils.dropItem(this.player, remaining);
    }

    /**
     * 将物品插入到潜影盒
     *
     * @return 物品剩余未插入的部分
     */
    @CheckReturnValue
    private ItemStack insertToShulkerBox(ItemStack itemStack) {
        if (CarpetOrgAdditionSettings.FAKE_PLAYER_SHULKER_BOX_ITEM_HANDLING.value()) {
            itemStack = itemStack.copyAndClear();
            // 所有潜影盒所在的索引
            ArrayList<Integer> shulkers = new ArrayList<>();
            for (int i = 0; i < this.getContainerSize(); i++) {
                ItemStack shulker = this.getItem(i);
                if (InventoryUtils.isShulkerBoxItem(shulker)) {
                    shulkers.add(i);
                    // 优先尝试向单一物品的潜影盒或杂物潜影盒装入物品
                    if (InventoryUtils.canAcceptAsSingleItemType(shulker, itemStack, false) || InventoryUtils.isJunkBox(shulker)) {
                        itemStack = InventoryUtils.addItemToShulkerBox(shulker, itemStack);
                        if (itemStack.isEmpty()) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }
            // 尝试向空潜影盒装入物品
            for (Integer index : shulkers) {
                ItemStack shulker = this.getItem(index);
                if (InventoryUtils.canAcceptAsSingleItemType(shulker, itemStack, true)) {
                    itemStack = InventoryUtils.addItemToShulkerBox(shulker, itemStack);
                    if (itemStack.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                }
            }
            if (!shulkers.isEmpty()) {
                // 拆分堆叠的空潜影盒
                int last = shulkers.getLast();
                if (last < this.getContainerSize() - 1 && this.getItem(last).getCount() > 1) {
                    this.splitStackedShulkerBox(last, last + 1);
                    itemStack = InventoryUtils.addItemToShulkerBox(this.getItem(last + 1), itemStack);
                }
            }
        }
        return itemStack;
    }

    /**
     * 向物品栏中插入物品
     */
    public void insert(ItemStack itemStack) {
        this.playerInventory.add(itemStack);
        if (itemStack.isEmpty()) {
            return;
        }
        ItemStack offHandStack = this.getStack(InteractionHand.OFF_HAND);
        if (offHandStack.isEmpty()) {
            this.setStack(InteractionHand.OFF_HAND, itemStack.copyAndClear());
        } else if (InventoryUtils.canMergeTo(itemStack, offHandStack)) {
            InventoryUtils.mergeStack(itemStack, offHandStack);
        }
    }

    /**
     * 拆分堆叠的潜影盒
     */
    private void splitStackedShulkerBox(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) {
            return;
        }
        ItemStack from = this.getItem(fromIndex);
        ItemStack to = this.getItem(toIndex);
        if (to.isEmpty()) {
            ItemStack split = from.split(1);
            this.setItem(toIndex, split);
        }
    }

    /**
     * 将指定物品栏移动到主手
     *
     * @return 是否移动成功
     */
    public boolean replenishment(Predicate<ItemStack> predicate) {
        return this.replenishment(InteractionHand.MAIN_HAND, predicate);
    }

    public boolean replenishment(InteractionHand hand, Predicate<ItemStack> predicate) {
        ItemStack stackInHand = this.getStack(hand);
        if (predicate.test(stackInHand)) {
            return true;
        }
        boolean pickItemFromShulker = CarpetOrgAdditionSettings.FAKE_PLAYER_SHULKER_BOX_ITEM_HANDLING.value();
        ArrayList<Integer> shulkers = new ArrayList<>();
        // 当前手槽位
        int headSlot = this.getHandSlotIndex(hand);
        for (int i = 0; i < this.getContainerSize(); i++) {
            if (i == headSlot) {
                continue;
            }
            if (predicate.test(this.getItem(i))) {
                this.swap(i, headSlot);
                return true;
            } else if (pickItemFromShulker) {
                ItemStack shulker = this.getItem(i);
                if (shulker.isEmpty()) {
                    continue;
                }
                if (InventoryUtils.isShulkerBoxItem(shulker)) {
                    shulkers.add(i);
                }
            }
        }
        // 从潜影盒获取物品
        if (pickItemFromShulker) {
            for (Integer index : shulkers) {
                ItemStack shulker = this.getItem(index);
                ItemStack picked = InventoryUtils.pickItemFromShulkerBox(shulker, predicate);
                if (picked.isEmpty()) {
                    continue;
                }
                this.player.setItemInHand(hand, picked);
                this.insertWithInventoryPriority(stackInHand);
                return true;
            }
        }
        return false;
    }

    public void merge(Predicate<ItemStack> predicate) {
        Container container = this.getMain();
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (this.isValidSlot(i)) {
                ItemStack itemStack = container.getItem(i);
                if (itemStack.isEmpty()) {
                    continue;
                }
                if (predicate.test(itemStack)) {
                    this.merge(i, itemStack);
                }
            }
        }
    }

    private int getHandSlotIndex(InteractionHand hand) {
        return switch (hand) {
            case MAIN_HAND -> this.playerInventory.getSelectedSlot();
            case OFF_HAND -> 36;
        };
    }

    /**
     * @return 物品栏中是否包含指定物品
     */
    public boolean contains(Predicate<ItemStack> predicate) {
        boolean pickItemFromShulker = CarpetOrgAdditionSettings.FAKE_PLAYER_SHULKER_BOX_ITEM_HANDLING.value();
        for (int i = 0; i < this.getContainerSize(); i++) {
            ItemStack itemStack = this.getItem(i);
            if (itemStack.isEmpty()) {
                continue;
            }
            if (predicate.test(itemStack)) {
                return true;
            }
            if (pickItemFromShulker && InventoryUtils.contains(itemStack, predicate)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isValidSlot(int index) {
        ItemStack itemStack = this.getItem(index);
        if (QuickShulkerScreenHandler.isOpenedShulkerBox(player, itemStack)) {
            return false;
        }
        return SortableContainer.super.isValidSlot(index);
    }

    /**
     * 将该物品栏的索引映射到完整玩家物品栏上
     */
    private int map(int index) {
        return this.indexMapping.getInt(index);
    }

    @Override
    public ServerPlayer getPlayer() {
        return this.player;
    }

    @Override
    public Inventory getPlayerInventory() {
        return this.playerInventory;
    }
}
