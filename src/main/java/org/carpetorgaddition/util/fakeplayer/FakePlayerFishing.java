package org.carpetorgaddition.util.fakeplayer;

import carpet.fakes.ServerPlayerInterface;
import carpet.helpers.EntityPlayerActionPack;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.carpetorgaddition.mixin.command.FishingBobberEntityAccessor;
import org.carpetorgaddition.util.fakeplayer.actiondata.FishingData;
import org.carpetorgaddition.util.wheel.SingleThingCounter;

public class FakePlayerFishing {
    private FakePlayerFishing() {
    }

    public static void fishing(FishingData fishingData, EntityPlayerMPFake fakePlayer) {
        SingleThingCounter timer = fishingData.getTimer();
        // 检查玩家是否持有钓鱼竿
        if (pickFishingRod(fakePlayer)) {
            // 检查玩家是否抛出钓竿
            FishingBobberEntity fishHook = fakePlayer.fishHook;
            if (fishHook == null) {
                // 检查计时器是否清零
                if (timer.isZero()) {
                    // 右键抛出钓鱼竿
                    use(fakePlayer);
                } else {
                    timer.decrement();
                }
            } else {
                // 如果钓鱼竿钩到方块或其它实体，通过切换物品收杆，防止额外的耐久损耗
                if (fishHook.isOnGround() || fishHook.getHookedEntity() != null) {
                    switchInventory(fakePlayer);
                }
                // 检查鱼是否上钩
                if (canReelInTheFishingPole(fishHook)) {
                    // 右键收杆
                    use(fakePlayer);
                    // 设置5个游戏刻后重新抛竿
                    timer.set(5);
                }
            }
        }
    }

    /**
     * 将钓鱼竿拿到手上
     *
     * @return 物品栏是否有钓鱼竿
     */
    private static boolean pickFishingRod(EntityPlayerMPFake fakePlayer) {
        // 如果玩家手上有钓鱼竿，无需切换
        if (fakePlayer.getMainHandStack().isOf(Items.FISHING_ROD) || fakePlayer.getOffHandStack().isOf(Items.FISHING_ROD)) {
            return true;
        }
        // 从物品栏拿取钓鱼竿
        PlayerInventory inventory = fakePlayer.getInventory();
        for (int i = 0; i < inventory.main.size(); i++) {
            if (inventory.getStack(i).isOf(Items.FISHING_ROD)) {
                // 将非钓鱼竿物品放入主手
                inventory.swapSlotWithHotbar(i);
                if (fakePlayer.getMainHandStack().isOf(Items.FISHING_ROD)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 通过切换物品栏收起钓鱼竿
     */
    private static void switchInventory(EntityPlayerMPFake fakePlayer) {
        if (fakePlayer.getOffHandStack().isOf(Items.FISHING_ROD)) {
            swapHands(fakePlayer);
        }
        PlayerInventory inventory = fakePlayer.getInventory();
        // 查找物品栏内的非钓鱼竿物品
        for (int i = 0; i < inventory.main.size(); i++) {
            ItemStack itemStack = inventory.getStack(i);
            // 其它钓鱼竿物品不能与主手物品切换
            if (itemStack.isOf(Items.FISHING_ROD)) {
                continue;
            }
            // 将非钓鱼竿物品放入主手
            inventory.swapSlotWithHotbar(i);
            // 检查钓鱼竿是否切换成功（钓鱼竿不能与盔甲槽中的物品切换），如果成功，结束方法
            if (fakePlayer.getMainHandStack().isOf(Items.FISHING_ROD)) {
                // 主手是钓鱼竿，切换失败
                continue;
            }
            return;
        }
    }

    /**
     * @return 是否可以收杆
     */
    private static boolean canReelInTheFishingPole(FishingBobberEntity fishHook) {
        return ((FishingBobberEntityAccessor) fishHook).getHookCountdown() > 0;
    }

    /**
     * 右键
     */
    private static void use(EntityPlayerMPFake fakePlayer) {
        EntityPlayerActionPack actionPack = ((ServerPlayerInterface) fakePlayer).getActionPack();
        actionPack.start(EntityPlayerActionPack.ActionType.USE, EntityPlayerActionPack.Action.once());
    }

    /**
     * 交换主副手物品
     */
    private static void swapHands(EntityPlayerMPFake fakePlayer) {
        EntityPlayerActionPack actionPack = ((ServerPlayerInterface) fakePlayer).getActionPack();
        actionPack.start(EntityPlayerActionPack.ActionType.SWAP_HANDS, EntityPlayerActionPack.Action.once());
    }
}
