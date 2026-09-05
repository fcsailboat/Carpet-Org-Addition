package boat.carpetorgaddition.wheel;

import boat.carpetorgaddition.util.InventoryUtils;
import boat.carpetorgaddition.wheel.inventory.AutoGrowInventory;
import boat.carpetorgaddition.wheel.inventory.PlayerStorageInventory;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class MenuController<T extends AbstractContainerMenu> {
    /**
     * 槽位外部的索引，相当于点击GUI外面，用来丢弃光标上的物品
     */
    public static final int EMPTY_SPACE_SLOT_INDEX = AbstractContainerMenu.SLOT_CLICKED_OUTSIDE;//-999
    /**
     * 模拟左键单击槽位
     */
    public static final int PICKUP_LEFT_CLICK = 0;
    /**
     * 模拟右键单击槽位
     */
    public static final int PICKUP_RIGHT_CLICK = 1;
    /**
     * 模拟按Q键丢弃物品
     */
    public static final int THROW_Q = 0;
    /**
     * 模拟Ctrl+Q丢弃物品
     */
    public static final int THROW_CTRL_Q = 1;
    private final T menu;
    private final EntityPlayerMPFake fakePlayer;
    private final PlayerStorageInventory inventory;

    public MenuController(T menu, EntityPlayerMPFake fakePlayer) {
        this.menu = menu;
        this.fakePlayer = fakePlayer;
        this.inventory = PlayerStorageInventory.of(fakePlayer);
    }

    /**
     * 模拟Ctrl+Q丢弃物品
     */
    public void drop(int index) {
        this.menu.clicked(index, THROW_CTRL_Q, ContainerInput.THROW, this.fakePlayer);
    }

    /**
     * 鼠标拾取并丢出物品
     * */
    public void pickupAndDrop(int index) {
        this.leftClick(index);
        this.leftClick(EMPTY_SPACE_SLOT_INDEX);
    }

    /**
     * 按住Shift快速移动物品
     */
    public ItemStack quickMove(int slotIndex) {
        return this.menu.quickMoveStack(this.fakePlayer, slotIndex);
    }

    /**
     * 将槽位上的物品放入玩家物品栏
     */
    public void recycling(int index) {
        if (this.menu.getSlot(index).hasItem()) {
            ItemStack before = this.menu.getCarried();
            if (!before.isEmpty()) {
                this.inventory.insertWithInventoryPriority(before);
            }
            this.leftClick(index);
            ItemStack after = this.menu.getCarried();
            if (!after.isEmpty()) {
                this.inventory.insertWithInventoryPriority(after);
            }
        }
    }

    public void collect(int index, AutoGrowInventory inventory) {
        InventoryUtils.assertEmptyStack(this.getCursorStack(), () -> "The item on the cursor is not empty");
        // 取出物品的过程中，输出槽位的物品可能会随着输入物品的改变而改变
        // 物品改变后，应停止取出物品，避免合成错误的物品
        Item item = this.menu.getSlot(index).getItem().getItem();
        while (true) {
            Slot slot = this.menu.getSlot(index);
            if (slot.hasItem() && slot.mayPickup(fakePlayer) && item == slot.getItem().getItem()) {
                // 拿取槽位上的物品
                this.leftClick(index);
                // 将槽位上的物品放入物品栏并清空光标上的物品
                inventory.addStack(this.getCursorStack());
                this.setCursorStack(ItemStack.EMPTY);
            } else {
                break;
            }
        }
    }

    public void dropCursorStack() {
        ItemStack itemStack = this.getCursorStack();
        if (itemStack.isEmpty()) {
            return;
        }
        this.menu.clicked(EMPTY_SPACE_SLOT_INDEX, PICKUP_LEFT_CLICK, ContainerInput.PICKUP, fakePlayer);
    }

    public ItemStack getCursorStack() {
        return this.menu.getCarried();
    }

    public void setCursorStack(ItemStack itemStack) {
        this.menu.setCarried(itemStack);
    }

    public EntityPlayerMPFake getFakePlayer() {
        return this.fakePlayer;
    }

    private void leftClick(int index) {
        this.menu.clicked(index, PICKUP_LEFT_CLICK, ContainerInput.PICKUP, this.fakePlayer);
    }
}
