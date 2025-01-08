package org.carpetorgaddition.periodic.fakeplayer;

import carpet.CarpetSettings;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.command.ServerCommandSource;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.exception.InfiniteLoopException;
import org.carpetorgaddition.util.InventoryUtils;
import org.carpetorgaddition.periodic.fakeplayer.actiondata.CraftingTableCraftData;
import org.carpetorgaddition.periodic.fakeplayer.actiondata.InventoryCraftData;
import org.carpetorgaddition.util.inventory.AutoGrowInventory;
import org.carpetorgaddition.util.wheel.ItemStackPredicate;

public class FakePlayerCraft {
    // 最大循环次数
    private static final int MAX_LOOP_COUNT = 1200;

    private FakePlayerCraft() {
    }

    // 在工作台合成物品
    public static void craftingTableCraft(CraftingTableCraftData craftData, EntityPlayerMPFake fakePlayer) {
        if (fakePlayer.currentScreenHandler instanceof CraftingScreenHandler craftingScreenHandler) {
            AutoGrowInventory inventory = new AutoGrowInventory();
            craftingTableCraft(craftData, fakePlayer, inventory, craftingScreenHandler);
            // 丢弃合成输出
            dropCraftOut(fakePlayer, inventory);
        }
    }

    // 在生存模式物品栏合成物品
    public static void inventoryCraft(InventoryCraftData craftData, EntityPlayerMPFake fakePlayer) {
        AutoGrowInventory inventory = new AutoGrowInventory();
        inventoryCraft(craftData, fakePlayer, inventory);
        // 丢弃合成输出
        dropCraftOut(fakePlayer, inventory);
    }

    private static void craftingTableCraft(
            CraftingTableCraftData craftData,
            EntityPlayerMPFake fakePlayer,
            AutoGrowInventory inventory,
            CraftingScreenHandler craftingScreenHandler
    ) {
        ItemStackPredicate[] items = craftData.getPredicates();
        // 定义变量记录成功完成合成的次数
        int craftCount = 0;
        // 记录循环次数用来在游戏可能进入死循环时抛出异常
        int loopCount = 0;
        do {
            // 检查循环次数，在循环次数过多时抛出异常
            loopCount++;
            if (loopCount > MAX_LOOP_COUNT) {
                throw new InfiniteLoopException();
            }
            // 定义变量记录找到正确合成材料的次数
            int successCount = 0;
            // 依次获取每一个合成材料和遍历合成格
            for (int index = 1; index <= 9; index++) {
                //依次获取每一个合成材料
                ItemStackPredicate matcher = items[index - 1];
                Slot slot = craftingScreenHandler.getSlot(index);
                // 如果合成格的指定槽位不是所需要合成材料，则丢出该物品
                if (slot.hasStack()) {
                    ItemStack itemStack = slot.getStack();
                    if (matcher.test(itemStack)) {
                        // 合成表格上已经有正确的合成材料，找到正确的合成材料次数自增
                        successCount++;
                    } else {
                        FakePlayerUtils.throwItem(craftingScreenHandler, index, fakePlayer);
                    }
                } else {
                    // 如果指定合成材料是空气，则不需要遍历物品栏，直接跳过该物品，并增加找到正确合成材料的次数
                    if (matcher.isEmpty()) {
                        successCount++;
                        continue;
                    }
                    // 遍历物品栏找到需要的合成材料
                    int size = craftingScreenHandler.slots.size();
                    for (int inventoryIndex = 10; inventoryIndex < size; inventoryIndex++) {
                        ItemStack itemStack = craftingScreenHandler.getSlot(inventoryIndex).getStack();
                        if (matcher.test(itemStack)) {
                            // 光标拾取和移动物品
                            if (FakePlayerUtils.withKeepPickupAndMoveItemStack(craftingScreenHandler,
                                    inventoryIndex, index, fakePlayer)) {
                                // 找到正确合成材料的次数自增
                                successCount++;
                                break;
                            }
                        } else if (CarpetOrgAdditionSettings.fakePlayerCraftPickItemFromShulkerBox
                                && InventoryUtils.isShulkerBoxItem(itemStack)) {
                            ItemStack contentItemStack = InventoryUtils.pickItemFromShulkerBox(itemStack, matcher);
                            if (!contentItemStack.isEmpty()) {
                                // 丢弃光标上的物品（如果有）
                                FakePlayerUtils.dropCursorStack(craftingScreenHandler, fakePlayer);
                                // 将光标上的物品设置为从潜影盒中取出来的物品
                                craftingScreenHandler.setCursorStack(contentItemStack);
                                // 将光标上的物品放在合成方格的槽位上
                                FakePlayerUtils.pickupCursorStack(craftingScreenHandler, index, fakePlayer);
                                successCount++;
                                break;
                            }
                        }
                        // 合成格没有遍历完毕，继续查找下一个合成材料
                        // 合成格遍历完毕，并且物品栏找不到需要的合成材料，结束方法
                        if (index == 9 && inventoryIndex == size - 1) {
                            return;
                        }
                    }
                }
            }
            // 正确材料找到的次数等于9说明全部找到，可以合成
            if (successCount == 9) {
                // 工作台输出槽里有物品，说明配方正确并且前面的合成没有问题，可以取出合成的物品
                if (craftingScreenHandler.getSlot(0).hasStack()) {
                    FakePlayerUtils.collectItem(craftingScreenHandler, 0, inventory, fakePlayer);
                    // 合成成功，合成计数器自增
                    craftCount++;
                    // 避免在一个游戏刻内合成太多物品造成巨量卡顿
                    if (shouldStop(craftCount)) {
                        return;
                    }
                } else {
                    // 如果没有输出物品，说明之前的合成步骤有误，停止合成
                    stopCraftAction(fakePlayer.getCommandSource(), fakePlayer);
                    return;
                }
            } else {
                if (successCount > 9) {
                    // 找到正确合成材料的次数不应该大于合成槽位数量，如果超过了说明前面的操作出了问题，抛出异常结束方法
                    throw new IllegalStateException(fakePlayer.getName().getString() + "找到正确合成材料的次数为"
                            + successCount + "，正常不应该超过9");
                }
                // 遍历完物品栏后，如果找到正确合成材料小于9，认为玩家身上没有足够的合成材料了，直接结束方法
                return;
            }
        } while (CarpetSettings.ctrlQCraftingFix);
    }

    private static void inventoryCraft(InventoryCraftData craftData, EntityPlayerMPFake fakePlayer, AutoGrowInventory inventory) {
        PlayerScreenHandler playerScreenHandler = fakePlayer.playerScreenHandler;
        ItemStackPredicate[] items = craftData.getPredicates();
        // 定义变量记录成功完成合成的次数
        int craftCount = 0;
        // 记录循环次数用来在游戏可能进入死循环时抛出异常
        int loopCount = 0;
        do {
            // 检查循环次数
            loopCount++;
            if (loopCount > MAX_LOOP_COUNT) {
                throw new InfiniteLoopException();
            }
            // 定义变量记录找到正确合成材料的次数
            int successCount = 0;
            // 遍历4x4合成格
            for (int craftIndex = 1; craftIndex <= 4; craftIndex++) {
                // 获取每一个合成材料
                ItemStackPredicate matcher = items[craftIndex - 1];
                Slot slot = playerScreenHandler.getSlot(craftIndex);
                // 检查合成格上是否已经有物品
                if (slot.hasStack()) {
                    // 如果有并且物品是正确的合成材料，直接结束本轮循环，即跳过该物品
                    if (matcher.test(slot.getStack())) {
                        successCount++;
                        continue;
                    } else {
                        // 如果不是，丢出该物品
                        FakePlayerUtils.throwItem(playerScreenHandler, craftIndex, fakePlayer);
                    }
                } else if (matcher.isEmpty()) {
                    successCount++;
                    continue;
                }
                int size = playerScreenHandler.slots.size();
                // 遍历物品栏，包括盔甲槽和副手槽
                for (int inventoryIndex = 5; inventoryIndex < size; inventoryIndex++) {
                    ItemStack itemStack = playerScreenHandler.getSlot(inventoryIndex).getStack();
                    // 如果该槽位是正确的合成材料，将该物品移动到合成格，然后增加找到正确合成材料的次数
                    if (matcher.test(itemStack)) {
                        if (FakePlayerUtils.withKeepPickupAndMoveItemStack(playerScreenHandler,
                                inventoryIndex, craftIndex, fakePlayer)) {
                            successCount++;
                            break;
                        }
                    } else if (CarpetOrgAdditionSettings.fakePlayerCraftPickItemFromShulkerBox
                            && InventoryUtils.isShulkerBoxItem(itemStack)) {
                        ItemStack contentItemStack = InventoryUtils.pickItemFromShulkerBox(itemStack, matcher);
                        if (!contentItemStack.isEmpty()) {
                            // 丢弃光标上的物品（如果有）
                            FakePlayerUtils.dropCursorStack(playerScreenHandler, fakePlayer);
                            // 将光标上的物品设置为从潜影盒中取出来的物品
                            playerScreenHandler.setCursorStack(contentItemStack);
                            // 将光标上的物品放在合成方格的槽位上
                            FakePlayerUtils.pickupCursorStack(playerScreenHandler, craftIndex, fakePlayer);
                            successCount++;
                            break;
                        }
                    }
                    // 如果遍历完物品栏还没有找到指定物品，认为玩家身上已经没有该物品，结束方法
                    if (craftIndex == 4 && inventoryIndex == size - 1) {
                        return;
                    }
                }
            }
            // 如果找到正确合成材料的次数为4，认为找到了所有的合成材料，尝试输出物品
            if (successCount == 4) {
                // 如果输出槽有物品，则丢出该物品
                if (playerScreenHandler.getSlot(0).hasStack()) {
                    FakePlayerUtils.collectItem(playerScreenHandler, 0, inventory, fakePlayer);
                    // 合成成功，合成计数器自增
                    craftCount++;
                    // 避免在一个游戏刻内合成太多物品造成巨量卡顿
                    if (shouldStop(craftCount)) {
                        return;
                    }
                } else {
                    // 如果输出槽没有物品，认为前面的合成操作有误，停止合成
                    stopCraftAction(fakePlayer.getCommandSource(), fakePlayer);
                    return;
                }
            } else {
                if (successCount > 4) {
                    // 找到正确合成材料的次数不应该大于合成槽位数量，如果超过了说明前面的操作出了问题，抛出异常结束方法
                    throw new IllegalStateException(fakePlayer.getName().getString() + "找到正确合成材料的次数为"
                            + successCount + "，正常不应该超过4");
                }
                // 遍历完物品栏后，如果没有找到足够多的合成材料，认为玩家身上没有足够的合成材料了，直接结束方法
                return;
            }
        } while (CarpetSettings.ctrlQCraftingFix);
    }

    // 丢弃合成输出
    private static void dropCraftOut(EntityPlayerMPFake fakePlayer, AutoGrowInventory inventory) {
        for (ItemStack itemStack : inventory) {
            fakePlayer.dropItem(itemStack, false, true);
        }
    }

    /**
     * 是否应该因为合成次数过多而停止合成
     *
     * @param craftCount 当前合成次数
     * @return 是否应该停止
     */
    private static boolean shouldStop(int craftCount) {
        if (CarpetOrgAdditionSettings.fakePlayerMaxCraftCount < 0) {
            return false;
        }
        return craftCount >= CarpetOrgAdditionSettings.fakePlayerMaxCraftCount;
    }

    /**
     * 假玩家停止物品合成操作，并广播停止合成的消息
     *
     * @param source       发送消息的消息源
     * @param playerMPFake 需要停止操作的假玩家
     */
    private static void stopCraftAction(ServerCommandSource source, EntityPlayerMPFake playerMPFake) {
        FakePlayerUtils.stopAction(source, playerMPFake, "carpet.commands.playerAction.craft");
    }
}
