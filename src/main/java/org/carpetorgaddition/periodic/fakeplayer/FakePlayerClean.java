package org.carpetorgaddition.periodic.fakeplayer;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import org.carpetorgaddition.periodic.fakeplayer.actiondata.CleanData;

public class FakePlayerClean {
    private FakePlayerClean() {
    }

    public static void clean(CleanData cleanData, EntityPlayerMPFake fakePlayer) {
        Item item = cleanData.isAllItem() ? null : cleanData.getItem();
        //判断假玩家打开的界面是不是潜影盒的GUI
        if (fakePlayer.currentScreenHandler instanceof ShulkerBoxScreenHandler shulkerBoxScreenHandler) {
            // 使用循环一次丢弃一组丢出潜影盒中的物品
            for (int index = 0; index < 27; index++) {
                ItemStack itemStack = shulkerBoxScreenHandler.getSlot(index).getStack();
                if (itemStack.isEmpty()) {
                    continue;
                }
                if (cleanData.isAllItem() || itemStack.isOf(item)) {
                    // 丢弃一组物品
                    FakePlayerUtils.throwItem(shulkerBoxScreenHandler, index, fakePlayer);
                }
            }
            // 物品全部丢出后自动关闭潜影盒
            fakePlayer.closeHandledScreen();
        }
    }
}
