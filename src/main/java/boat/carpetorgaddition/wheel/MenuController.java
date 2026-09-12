package boat.carpetorgaddition.wheel;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.util.InventoryUtils;
import boat.carpetorgaddition.wheel.inventory.AutoGrowInventory;
import boat.carpetorgaddition.wheel.inventory.PlayerStorageInventory;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.core.NonNullList;
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
    public static final int EMPTY_SPACE_SLOT_INDEX = AbstractContainerMenu.SLOT_CLICKED_OUTSIDE; // -999
    /**
     * 左键单击
     */
    public static final int PICKUP_LEFT_CLICK = 0;
    /**
     * 右键单击
     */
    public static final int PICKUP_RIGHT_CLICK = 1;
    /**
     * Q键丢弃物品
     */
    public static final int THROW_Q = 0;
    /**
     * Ctrl+Q丢弃物品
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
     * 按下Ctrl+Q丢弃物品
     */
    public void dropAll(int index) {
        this.menu.clicked(index, THROW_CTRL_Q, ContainerInput.THROW, this.fakePlayer);
    }

    /**
     * 按下Q丢弃物品
     */
    public void dropOne(int index) {
        this.menu.clicked(index, THROW_Q, ContainerInput.THROW, this.fakePlayer);
    }

    /**
     * 鼠标拾取并丢出物品
     * */
    public void dropAllByPickup(int index) {
        this.leftClick(index);
        this.leftClick(EMPTY_SPACE_SLOT_INDEX);
    }

    /**
     * 按住Shift快速移动物品
     *
     * @return 当没有物品移动时，返回{@link ItemStack#EMPTY}，否则返回原物品
     */
    public ItemStack quickMove(int index) {
        return this.menu.quickMoveStack(this.fakePlayer, index);
    }

    /**
     * 将槽位上的物品放入玩家物品栏
     */
    public void moveSlotStackToInventory(int index) {
        if (this.menu.getSlot(index).hasItem()) {
            this.leftClick(index);
            this.moveCursorStackToInventory();
            // 如果一开始光标上有物品，再次单击可以重新拿起物品
            this.leftClick(index);
        }
    }

    /**
     * 将鼠标光标上的物品放入物品栏
     */
    public void moveCursorStackToInventory() {
        ItemStack cursorStack = this.getCursorStack();
        if (cursorStack.isEmpty()) {
            return;
        }
        this.inventory.insertWithInventoryPriority(cursorStack);
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

    /**
     * 将物品从一个槽位移动到另一个槽位
     * @return 物品是否可以移动
     */
    public boolean moveItemStack(int fromIndex, int toIndex) {
        ItemStack itemStack = this.menu.getSlot(fromIndex).getItem();
        boolean keep = CarpetOrgAdditionSettings.FAKE_PLAYER_ACTION_KEEP_ITEM.value();
        if (keep && itemStack.getCount() == 1 && itemStack.getMaxStackSize() > 1) {
            return false;
        }
        this.moveCursorStackToInventory();
        this.leftClick(fromIndex);
        if (keep && this.menu.getCarried().getMaxStackSize() > 1) {
            // 放回一个物品
            this.rightClick(fromIndex);
        }
        this.leftClick(toIndex);
        return true;
    }

    /**
     * 比较并丢出槽位物品<br>
     * 如果槽位上的物品与预期物品相同，则丢出槽位上的物品
     *
     * @see <a href="https://bugs.mojang.com/browse/MC-157977">MC-157977</a>
     * @see <a href="https://bugs.mojang.com/browse/MC-215441">MC-215441</a>
     */
    public void compareAndDrop(int index, ItemStack itemStack, boolean once) {
        InventoryUtils.assertEmptyStack(this.getCursorStack());
        Slot slot = this.getSlot(index);
        while (slot.hasItem() && ItemStack.isSameItemSameComponents(itemStack, slot.getItem()) && slot.mayPickup(this.fakePlayer)) {
            this.dropOne(index);
            if (once) {
                break;
            }
        }
    }

    /**
     * 左键单击槽位
     */
    public void leftClick(int index) {
        this.menu.clicked(index, PICKUP_LEFT_CLICK, ContainerInput.PICKUP, this.fakePlayer);
    }

    /**
     * 右键单击槽位
     */
    public void rightClick(int index) {
        this.menu.clicked(index, PICKUP_RIGHT_CLICK, ContainerInput.PICKUP, this.fakePlayer);
    }

    /**
     * 丢弃光标上的物品
     */
    @SuppressWarnings("unused")
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

    public Slot getSlot(int index) {
        return this.menu.getSlot(index);
    }

    public ItemStack getSlotStack(int index) {
        return this.getSlot(index).getItem();
    }

    public T getMenu() {
        return this.menu;
    }

    public EntityPlayerMPFake getFakePlayer() {
        return this.fakePlayer;
    }

    public NonNullList<Slot> getSlots() {
        return this.menu.slots;
    }

    public PlayerStorageInventory getInventory() {
        return this.inventory;
    }
}
