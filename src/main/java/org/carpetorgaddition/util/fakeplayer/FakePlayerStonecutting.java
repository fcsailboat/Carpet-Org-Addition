package org.carpetorgaddition.util.fakeplayer;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.StonecutterScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.exception.InfiniteLoopException;
import org.carpetorgaddition.util.InventoryUtils;
import org.carpetorgaddition.util.fakeplayer.actiondata.StonecuttingData;
import org.carpetorgaddition.util.inventory.AutoGrowInventory;

public class FakePlayerStonecutting {
    private FakePlayerStonecutting() {
    }

    // TODO 检查“假玩家合成保留物品”是否对该功能有效
    public static void stonecutting(StonecuttingData stonecuttingData, EntityPlayerMPFake fakePlayer) {
        /*
         * 切石机的输出槽不能使用Ctrl+Q一次性丢出整组物品，只能一个个丢出。在合成
         * 物品时，会有大量物品产生，因为是一个个丢出的，所以物品不会立即合并。例如，
         * 一组石头合成石砖时，不会生成一整组的石砖，而是生成64个未堆叠的石砖，合成时，
         * 会因为物品实体过多，导致巨量卡顿，甚至游戏卡死。因此，这里加一个物品栏用来
         * 临时保存合成输出的物品，在物品栏内完成物品的合并，并在本tick的合成结束时
         * 一次性丢出物品栏内的所有物品，这样丢出的物品就是已经合并的，并显著减少卡顿
         */
        AutoGrowInventory inventory = new AutoGrowInventory();
        // 合成物品
        stonecutting(stonecuttingData, fakePlayer, inventory);
        // 丢弃合成输出
        for (ItemStack itemStack : inventory) {
            fakePlayer.dropItem(itemStack, false, true);
        }
    }

    public static void stonecutting(StonecuttingData stonecuttingData, EntityPlayerMPFake fakePlayer, AutoGrowInventory inventory) {
        if (fakePlayer.currentScreenHandler instanceof StonecutterScreenHandler stonecutterScreenHandler) {
            // 获取要切割的物品和按钮的索引
            Item item = stonecuttingData.getItem();
            int buttonIndex = stonecuttingData.getButton();
            // 定义变量记录成功完成合成的次数
            int craftCount = 0;
            // 用于循环次数过多时抛出异常结束循环
            int loopCount = 0;
            while (true) {
                loopCount++;
                if (loopCount > 1000) {
                    throw new InfiniteLoopException();
                }
                // 定义变量记录是否需要遍历物品栏
                boolean needToTraverseInventory = true;
                // 获取切石机输入槽对象
                Slot inputSlot = stonecutterScreenHandler.getSlot(0);
                // 判断切石机输入槽是否有物品
                if (inputSlot.hasStack()) {
                    // 如果有物品，并且是指定物品，设置不需要遍历物品栏
                    ItemStack itemStack = inputSlot.getStack();
                    if (itemStack.isOf(item)) {
                        needToTraverseInventory = false;
                    } else {
                        // 如果不是指定物品，丢出该物品
                        FakePlayerUtils.throwItem(stonecutterScreenHandler, 0, fakePlayer);
                    }
                }
                // 如果需要遍历物品栏
                if (needToTraverseInventory) {
                    // 尝试从物品栏中找到需要的物品
                    if (tryMoveItem(fakePlayer, stonecutterScreenHandler, item)) {
                        return;
                    }
                }
                // 模拟单击切石机按钮
                stonecutterScreenHandler.onButtonClick(fakePlayer, buttonIndex);
                // 获取切石机输出槽对象
                Slot outputSlot = stonecutterScreenHandler.getSlot(1);
                // 如果输出槽有物品
                if (outputSlot.hasStack()) {
                    // 收集产物
                    FakePlayerUtils.collectItem(stonecutterScreenHandler, 1, inventory, fakePlayer);
                    craftCount++;
                    // 限制每个游戏刻合成次数
                    int ruleValue = CarpetOrgAdditionSettings.fakePlayerMaxCraftCount;
                    if (ruleValue > 0 && craftCount >= CarpetOrgAdditionSettings.fakePlayerMaxCraftCount) {
                        return;
                    }
                } else {
                    // 否则，认为前面的操作有误，停止合成，结束方法
                    FakePlayerUtils.stopAction(fakePlayer.getCommandSource(), fakePlayer, "carpet.commands.playerAction.stone_cutting");
                    return;
                }
            }
        }
    }

    /**
     * 将物品移动到切石机输入槽位
     *
     * @return 是否已经所有材料用完
     */
    private static boolean tryMoveItem(EntityPlayerMPFake fakePlayer, StonecutterScreenHandler screenHandler, Item item) {
        for (int index = 2; index < screenHandler.slots.size(); index++) {
            // 如果找到，移动到切石机输入槽，然后结束循环
            ItemStack itemStack = screenHandler.getSlot(index).getStack();
            if (itemStack.isOf(item)) {
                FakePlayerUtils.quickMove(screenHandler, index, fakePlayer);
                return false;
            } else if (CarpetOrgAdditionSettings.fakePlayerCraftPickItemFromShulkerBox && InventoryUtils.isShulkerBoxItem(itemStack)) {
                // 从潜影盒中查找指定物品
                ItemStack stack = InventoryUtils.pickItemFromShulkerBox(itemStack, content -> content.isOf(item));
                // 未找到指定物品
                if (stack.isEmpty()) {
                    continue;
                }
                // 将物品移动到切石机输入槽
                // 丢弃光标上的物品（如果有）
                FakePlayerUtils.dropCursorStack(screenHandler, fakePlayer);
                // 将光标上的物品设置为从潜影盒中取出来的物品
                screenHandler.setCursorStack(stack);
                // 将光标上的物品放在切石机输入槽位上
                FakePlayerUtils.pickupCursorStack(screenHandler, 0, fakePlayer);
                return false;
            }
        }
        // 如果遍历完物品栏还没有找到指定物品，认为物品栏中没有该物品，结束方法
        return true;
    }
}
