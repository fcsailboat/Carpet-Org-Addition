package org.carpetorgaddition.periodic.fakeplayer;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.carpetorgaddition.periodic.fakeplayer.actiondata.FillData;

public class FakePlayerFill {
    private FakePlayerFill() {
    }

    public static void fill(FillData fillData, EntityPlayerMPFake fakePlayer) {
        ScreenHandler screenHandler = fakePlayer.currentScreenHandler;
        if (screenHandler == null || screenHandler instanceof PlayerScreenHandler) {
            return;
        }
        boolean allItem = fillData.isAllItem();
        // 获取要装在潜影盒的物品
        Item item = allItem ? null : fillData.getItem();
        for (int index = 0; index < screenHandler.slots.size(); index++) {
            Slot slot = screenHandler.getSlot(index);
            // TODO 只支持常用容器
            if (slot.inventory instanceof PlayerInventory) {
                ItemStack itemStack = slot.getStack();
                if (itemStack.isEmpty()) {
                    continue;
                }
                if ((allItem || itemStack.isOf(item))) {
                    if (screenHandler instanceof ShulkerBoxScreenHandler && !itemStack.getItem().canBeNested()) {
                        continue;
                    }
                    // 模拟按住Shift键移动物品
                    if (FakePlayerUtils.quickMove(screenHandler, index, fakePlayer).isEmpty()) {
                        fakePlayer.onHandledScreenClosed();
                        return;
                    }
                }
            }
        }
    }
}
