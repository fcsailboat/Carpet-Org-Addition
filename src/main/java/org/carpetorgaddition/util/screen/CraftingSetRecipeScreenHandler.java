package org.carpetorgaddition.util.screen;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.SlotActionType;
import org.carpetorgaddition.util.fakeplayer.FakePlayerAction;
import org.carpetorgaddition.util.fakeplayer.FakePlayerActionInterface;
import org.carpetorgaddition.util.fakeplayer.FakePlayerActionManager;
import org.carpetorgaddition.util.fakeplayer.actiondata.CraftingTableCraftData;
import org.carpetorgaddition.util.fakeplayer.actiondata.InventoryCraftData;
import org.carpetorgaddition.util.matcher.ItemMatcher;

public class CraftingSetRecipeScreenHandler extends CraftingScreenHandler implements UnavailableSlotSyncInterface {
    /**
     * 一个假玩家对象，类中所有操作都是围绕这个假玩家进行的
     */
    private final EntityPlayerMPFake fakePlayer;

    public CraftingSetRecipeScreenHandler(
            int syncId,
            PlayerInventory playerInventory,
            EntityPlayerMPFake fakePlayer,
            ScreenHandlerContext screenHandlerContext) {
        super(syncId, playerInventory, screenHandlerContext
        );
        this.fakePlayer = fakePlayer;
    }

    // 阻止玩家取出输出槽位的物品
    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex == 0) {
            return;
        }
        super.onSlotClick(slotIndex, button, actionType, player);
    }

    // 关闭GUI时，设置假玩家的合成动作和配方
    @Override
    public void onClosed(PlayerEntity player) {
        //如果没有给假玩家指定合成配方，结束方法
        if (craftingInventory.isEmpty()) {
            return;
        }
        //修改假玩家的3x3合成配方
        Item[] items = new Item[9];
        for (int i = 0; i < craftingInventory.size(); i++) {
            items[i] = craftingInventory.getStack(i).getItem();
        }
        // 设置假玩家合成动作
        setCraftAction(items, FakePlayerActionInterface.getManager(fakePlayer));
        // 关闭GUI后，使用父类的方法让物品回到玩家背包
        super.onClosed(player);
    }

    // 设置假玩家合成动作
    private void setCraftAction(Item[] items, FakePlayerActionManager actionManager) {
        // 如果能在2x2合成格中合成，优先使用2x2
        if (canInventoryCraft(items, 0, 1, 2, 5, 8)) {
            actionManager.setAction(FakePlayerAction.INVENTORY_CRAFT, createData(items, 3, 4, 6, 7));
        } else if (canInventoryCraft(items, 0, 3, 6, 7, 8)) {
            actionManager.setAction(FakePlayerAction.INVENTORY_CRAFT, createData(items, 1, 2, 4, 5));
        } else if (canInventoryCraft(items, 2, 5, 6, 7, 8)) {
            actionManager.setAction(FakePlayerAction.INVENTORY_CRAFT, createData(items, 0, 1, 3, 4));
        } else if (canInventoryCraft(items, 0, 1, 2, 3, 6)) {
            actionManager.setAction(FakePlayerAction.INVENTORY_CRAFT, createData(items, 4, 5, 7, 8));
        } else {
            //将假玩家动作设置为3x3合成
            ItemMatcher[] itemMatchersArr = new ItemMatcher[9];
            for (int i = 0; i < itemMatchersArr.length; i++) {
                itemMatchersArr[i] = new ItemMatcher(items[i]);
            }
            actionManager.setAction(FakePlayerAction.CRAFTING_TABLE_CRAFT, new CraftingTableCraftData(itemMatchersArr));
        }
    }

    // 可以在物品栏合成
    private boolean canInventoryCraft(Item[] items, int... indices) {
        for (int index : indices) {
            if (items[index] == Items.AIR) {
                continue;
            }
            return false;
        }
        return true;
    }

    // 创建合成数据
    private InventoryCraftData createData(Item[] items, int... indices) {
        ItemMatcher[] matchers = new ItemMatcher[4];
        // 这里的index并不是indices里保存的元素
        for (int index = 0; index < 4; index++) {
            matchers[index] = new ItemMatcher(items[indices[index]]);
        }
        return new InventoryCraftData(matchers);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public int from() {
        return this.getCraftingResultSlotIndex();
    }

    @Override
    public int to() {
        return this.getCraftingResultSlotIndex();
    }
}
