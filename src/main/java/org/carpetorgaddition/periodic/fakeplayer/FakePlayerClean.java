package org.carpetorgaddition.periodic.fakeplayer;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import org.carpetorgaddition.periodic.fakeplayer.actiondata.CleanData;

public class FakePlayerClean {
    private FakePlayerClean() {
    }

    public static void clean(CleanData cleanData, EntityPlayerMPFake fakePlayer) {
        Item item = cleanData.isAllItem() ? null : cleanData.getItem();
        ScreenHandler screenHandler = fakePlayer.currentScreenHandler;
        if (screenHandler == null || screenHandler instanceof PlayerScreenHandler) {
            return;
        }
        for (int index = 0; index < screenHandler.slots.size(); index++) {
            // 如果遍历到了玩家物品栏槽位，直接结束循环，因为后面一般不会再有容器槽位了
            // 合成器的输出槽位虽然在玩家物品栏槽位后面，但是这个槽位的物品无法取出，因此可以忽略
            if (screenHandler.getSlot(index).inventory instanceof PlayerInventory) {
                break;
            }
            ItemStack itemStack = screenHandler.getSlot(index).getStack();
            if (itemStack.isEmpty() || FakePlayerUtils.isGcaItem(itemStack)) {
                continue;
            }
            if (cleanData.isAllItem() || itemStack.isOf(item)) {
                // 丢弃一组物品
                FakePlayerUtils.throwItem(screenHandler, index, fakePlayer);
            }
        }
        // 物品全部丢出后自动关闭容器
        fakePlayer.closeHandledScreen();
    }
}
