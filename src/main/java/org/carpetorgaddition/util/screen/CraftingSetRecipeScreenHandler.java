package org.carpetorgaddition.util.screen;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.SlotActionType;
import org.carpetorgaddition.periodic.fakeplayer.action.FakePlayerAction;
import org.carpetorgaddition.periodic.fakeplayer.action.FakePlayerActionManager;
import org.carpetorgaddition.periodic.fakeplayer.action.context.CraftingTableCraftContext;
import org.carpetorgaddition.periodic.fakeplayer.action.context.InventoryCraftContext;
import org.carpetorgaddition.util.GenericFetcherUtils;
import org.carpetorgaddition.util.wheel.ItemStackPredicate;

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
        super(syncId, playerInventory, screenHandlerContext);
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
        setCraftAction(items, GenericFetcherUtils.getFakePlayerActionManager(fakePlayer));
        // 关闭GUI后，使用父类的方法让物品回到玩家背包
        super.onClosed(player);
    }

    // 设置假玩家合成动作
    private void setCraftAction(Item[] items, FakePlayerActionManager actionManager) {
        // 如果能在2x2合成格中合成，优先使用2x2
        if (canInventoryCraft(items, 0, 1, 2, 5, 8)) {
            actionManager.setAction(FakePlayerAction.INVENTORY_CRAFTING, createData(items, 3, 4, 6, 7));
        } else if (canInventoryCraft(items, 0, 3, 6, 7, 8)) {
            actionManager.setAction(FakePlayerAction.INVENTORY_CRAFTING, createData(items, 1, 2, 4, 5));
        } else if (canInventoryCraft(items, 2, 5, 6, 7, 8)) {
            actionManager.setAction(FakePlayerAction.INVENTORY_CRAFTING, createData(items, 0, 1, 3, 4));
        } else if (canInventoryCraft(items, 0, 1, 2, 3, 6)) {
            actionManager.setAction(FakePlayerAction.INVENTORY_CRAFTING, createData(items, 4, 5, 7, 8));
        } else {
            //将假玩家动作设置为3x3合成
            ItemStackPredicate[] predicates = new ItemStackPredicate[9];
            for (int i = 0; i < predicates.length; i++) {
                predicates[i] = new ItemStackPredicate(items[i]);
            }
            actionManager.setAction(FakePlayerAction.CRAFTING_TABLE_CRAFT, new CraftingTableCraftContext(predicates));
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
    private InventoryCraftContext createData(Item[] items, int... indices) {
        ItemStackPredicate[] predicates = new ItemStackPredicate[4];
        // 这里的index并不是indices里保存的元素
        for (int index = 0; index < 4; index++) {
            predicates[index] = new ItemStackPredicate(items[indices[index]]);
        }
        return new InventoryCraftContext(predicates);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public int from() {
        return 0;
    }

    @Override
    public int to() {
        return 0;
    }
}
