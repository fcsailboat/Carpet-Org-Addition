package org.carpetorgaddition.periodic.fakeplayer;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.command.ServerCommandSource;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.periodic.fakeplayer.actiondata.StopData;
import org.carpetorgaddition.util.InventoryUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.inventory.AutoGrowInventory;

public class FakePlayerUtils {

    /**
     * 槽位外部的索引，相当于点击GUI外面，用来丢弃光标上的物品
     */
    public static final int EMPTY_SPACE_SLOT_INDEX = ScreenHandler.EMPTY_SPACE_SLOT_INDEX;//-999
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

    private FakePlayerUtils() {
    }

    /**
     * 丢弃物品<br/>
     * 将要丢弃的物品堆栈对象复制一份并丢出，然后将原本的物品堆栈对象删除，在容器中，使用这种方式丢弃物品不会更新比较器，如果是工作台的输出槽，也不会自动清空合成槽内的物品，因此，不建议使用本方法操作GUI
     *
     * @param player    当前要丢弃物品的玩家
     * @param itemStack 要丢弃的物品堆栈对象
     */
    public static void dropItem(EntityPlayerMPFake player, ItemStack itemStack) {
        player.dropItem(itemStack.copy(), false, false);
        itemStack.setCount(0);
    }

    /**
     * 模拟Ctrl+Q丢弃物品<br/>
     * 如果当前槽位索引为0，表示按Ctrl+Q丢弃0索引槽位的物品<br/>
     * 使用这种方式丢弃物品可以更新比较器，如果需要丢出一些功能方块输出槽位的物品，例如工作台或切石机的输出槽位，应该使用本方法，因为这会同时清除合成槽位的物品
     *
     * @param screenHandler 假玩家当前打开的GUI
     * @param slotIndex     假玩家当前操作槽位的索引
     * @param player        当前操作的假玩家
     */
    public static void throwItem(ScreenHandler screenHandler, int slotIndex, EntityPlayerMPFake player) {
        screenHandler.onSlotClick(slotIndex, THROW_CTRL_Q, SlotActionType.THROW, player);
    }

    /**
     * 让假玩家停止当前的操作
     *
     * @param source       用来获取玩家管理器对象，然后通过玩家管理器发送消息，source本身不需要发送消息
     * @param playerMPFake 要停止操作的假玩家
     * @param key          停止操作时在聊天栏输出的内容的翻译键
     */
    public static void stopAction(ServerCommandSource source, EntityPlayerMPFake playerMPFake, String key, Object... obj) {
        ((FakePlayerActionInterface) playerMPFake).getActionManager().setAction(FakePlayerAction.STOP, StopData.STOP);
        MessageUtils.broadcastMessage(
                source.getServer(),
                TextUtils.appendAll(playerMPFake.getDisplayName(), ": ", TextUtils.translate(key, obj))
        );
    }

    /**
     * 模拟按住Shift快速移动物品
     *
     * @param screenHandler 当前打开的GUI
     * @param slotIndex     要操作的槽位的索引
     * @param player        当前操作的玩家
     */
    public static void quickMove(ScreenHandler screenHandler, int slotIndex, EntityPlayerMPFake player) {
        screenHandler.quickMove(player, slotIndex);
    }

    /**
     * 模拟光标拾取并丢出物品，作用与{@link #throwItem(ScreenHandler, int, EntityPlayerMPFake)}模拟Ctrl+Q丢出物品类似，但是可能有些GUI的槽位不能使用Ctrl+Q丢弃物品，这时可以尝试使用本方法
     *
     * @param screenHandler 玩家当前打开的GUI
     * @param slotIndex     玩家当前操作的索引
     * @param player        当前操作GUI的假玩家
     */
    public static void pickupAndThrow(ScreenHandler screenHandler, int slotIndex, EntityPlayerMPFake player) {
        screenHandler.onSlotClick(slotIndex, PICKUP_LEFT_CLICK, SlotActionType.PICKUP, player);
        screenHandler.onSlotClick(EMPTY_SPACE_SLOT_INDEX, PICKUP_LEFT_CLICK, SlotActionType.PICKUP, player);
    }

    /**
     * 通过模拟光标拾取放置物品来快速移动物品，如果光标在拾取物品前有物品，则先丢弃该物品，用这种方式移动物品比模拟按住Shift键移动和使用数字键移动更加灵活，因为它可以在任意两个槽位之间移动，但是这种移动方式需要点击插槽两次，比另外两种略微浪费资源，有条件时也可以使用另外两种<br/>
     * 此方法受到规则{@link CarpetOrgAdditionSettings#fakePlayerCraftKeepItem}影响，会先判断当前物品是否不能移动，然后再进行移动物品操作
     *
     * @param screenHandler 玩家当前打开的GUI
     * @param fromIndex     玩家拿取物品槽位的索引索引
     * @param toIndex       玩家放置物品的槽位所以
     * @param player        当前操作GUI的假玩家
     * @return 物品是否可以移动
     */
    public static boolean withKeepPickupAndMoveItemStack(ScreenHandler screenHandler, int fromIndex,
                                                         int toIndex, EntityPlayerMPFake player) {
        ItemStack itemStack = screenHandler.getSlot(fromIndex).getStack();
        // 如果假玩家合成保留物品启用，并且该物品的数量为1，并且该物品的最大堆叠数大于1
        // 认为这个物品需要保留，不移动物品
        if (CarpetOrgAdditionSettings.fakePlayerCraftKeepItem && itemStack.getCount() == 1 && itemStack.getMaxCount() > 1) {
            return false;
        }
        // 如果鼠标光标上有物品，先把光标上的物品丢弃
        if (!screenHandler.getCursorStack().isEmpty()) {
            screenHandler.onSlotClick(EMPTY_SPACE_SLOT_INDEX, PICKUP_LEFT_CLICK, SlotActionType.PICKUP, player);
        }
        screenHandler.onSlotClick(fromIndex, PICKUP_LEFT_CLICK, SlotActionType.PICKUP, player);
        // 如果规则假玩家合成保留物品启用，并且该物品的最大堆叠数大于1，就在该槽位上再放回一个物品
        if (CarpetOrgAdditionSettings.fakePlayerCraftKeepItem && screenHandler.getCursorStack().getMaxCount() > 1) {
            screenHandler.onSlotClick(fromIndex, PICKUP_RIGHT_CLICK, SlotActionType.PICKUP, player);
        }
        screenHandler.onSlotClick(toIndex, PICKUP_LEFT_CLICK, SlotActionType.PICKUP, player);
        return true;
    }

    /**
     * 功能与{@link FakePlayerUtils#withKeepPickupAndMoveItemStack(ScreenHandler, int, int, EntityPlayerMPFake)}基本一致，只是本方法使用右键拿取物品，即一次拿取一半的物品
     *
     * @param screenHandler 玩家当前打开的GUI
     * @param fromIndex     从哪个槽位拿取物品
     * @param toIndex       将物品放在哪个槽位
     * @param player        操作GUI的假玩家
     */
    @SuppressWarnings("unused")
    public static void pickupAndMoveHalfItemStack(ScreenHandler screenHandler, int fromIndex,
                                                  int toIndex, EntityPlayerMPFake player) {
        // 如果鼠标光标上有物品，先把光标上的物品丢弃
        if (!screenHandler.getCursorStack().isEmpty()) {
            screenHandler.onSlotClick(EMPTY_SPACE_SLOT_INDEX, PICKUP_LEFT_CLICK, SlotActionType.PICKUP, player);
        }
        // 右击拾取物品
        screenHandler.onSlotClick(fromIndex, PICKUP_RIGHT_CLICK, SlotActionType.PICKUP, player);
        // 放置物品依然是左键单击
        screenHandler.onSlotClick(toIndex, PICKUP_LEFT_CLICK, SlotActionType.PICKUP, player);
    }

    /**
     * 使用循环一个个丢弃槽位中的物品<br/>
     * 如果有些功能方块的输出槽位既不能使用Ctrl+Q丢弃，也不能使用鼠标拿起再丢弃，如切石机的输出槽，那么可以尝试使用本方法，使用时，应先确定确实不能使用上述两种方法进行丢弃，相比前面两种只需要一次操作的方法，本方法需要多次丢弃物品，这会更加消耗性能，增加游戏卡顿，因此，当前两种方法可用时，应使用前两种
     *
     * @param screenHandler 玩家当前打开的GUI
     * @param slotIndex     玩家当前操作槽位的索引
     * @param player        当前操作该GUI的玩家
     * @apiNote 请勿在工作台输出槽中使用此方法丢弃物品
     */
    public static void loopThrowItem(ScreenHandler screenHandler, int slotIndex, EntityPlayerMPFake player) {
        while (screenHandler.getSlot(slotIndex).hasStack()) {
            screenHandler.onSlotClick(slotIndex, THROW_Q, SlotActionType.THROW, player);
        }
    }

    /**
     * 收集槽位上的物品
     *
     * @throws IllegalStateException 如果调用时光标上存在物品
     */
    public static void collectItem(ScreenHandler screenHandler, int slotIndex, AutoGrowInventory inventory, EntityPlayerMPFake fakePlayer) {
        InventoryUtils.assertEmptyStack(screenHandler.getCursorStack(), () -> "光标上物品非空");
        while (screenHandler.getSlot(slotIndex).hasStack()) {
            // 拿取槽位上的物品
            screenHandler.onSlotClick(slotIndex, PICKUP_RIGHT_CLICK, SlotActionType.PICKUP, fakePlayer);
            // 将槽位上的物品放入物品栏并清空光标上的物品
            inventory.addStack(screenHandler.getCursorStack());
            screenHandler.setCursorStack(ItemStack.EMPTY);
            InventoryUtils.assertEmptyStack(screenHandler.getCursorStack(), () -> "物品未完全收集");
        }
    }

    /**
     * 丢弃光标上的物品<br/>
     * 该物品是玩家鼠标光标上正在被拎起的物品，它会影响玩家对GUI的其它操作，在进行其他操作如向光标上放置物品前应先丢弃光标上的物品
     *
     * @param screenHandler 玩家当前打开的GUI
     * @param fakePlayer    当前操作该GUI的玩家
     */
    public static void dropCursorStack(ScreenHandler screenHandler, EntityPlayerMPFake fakePlayer) {
        ItemStack itemStack = screenHandler.getCursorStack();
        if (itemStack.isEmpty()) {
            return;
        }
        screenHandler.onSlotClick(EMPTY_SPACE_SLOT_INDEX, PICKUP_LEFT_CLICK, SlotActionType.PICKUP, fakePlayer);
    }

    /**
     * 将光标上的物品放置在槽位中<br/>
     * 此方法不会检查对应槽位上有没有物品，因此使用该方法前应保证要放置物品的槽位上没有物品
     *
     * @param screenHandler 玩家当前打开的GUI
     * @param index         要放置物品的槽位索引
     * @param fakePlayer    当前操作该GUI的玩家
     */
    public static void pickupCursorStack(ScreenHandler screenHandler, int index, EntityPlayerMPFake fakePlayer) {
        screenHandler.onSlotClick(index, PICKUP_LEFT_CLICK, SlotActionType.PICKUP, fakePlayer);
    }
}