package org.carpetorgaddition.util.screen;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.StonecutterScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerAction;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerActionInterface;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerActionManager;
import org.carpetorgaddition.periodic.fakeplayer.actiondata.StonecuttingData;

public class StonecutterSetRecipeScreenHandler extends StonecutterScreenHandler implements UnavailableSlotSyncInterface{
    private final EntityPlayerMPFake fakePlayer;

    public StonecutterSetRecipeScreenHandler(
            int syncId,
            PlayerInventory playerInventory,
            ScreenHandlerContext screenHandlerContext,
            EntityPlayerMPFake fakePlayer
    ) {
        super(syncId, playerInventory, screenHandlerContext);
        this.fakePlayer = fakePlayer;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        // 不能单击输出槽位
        if (slotIndex == 1) {
            return;
        }
        super.onSlotClick(slotIndex, button, actionType, player);
    }

    @Override
    public void onClosed(PlayerEntity player) {
        ItemStack itemStack = this.input.getStack(0);
        if (itemStack.isEmpty()) {
            return;
        }
        // 获取按钮索引
        int button = this.getSelectedRecipe();
        if (button == -1) {
            return;
        }
        FakePlayerActionManager actionManager = FakePlayerActionInterface.getManager(this.fakePlayer);
        // 设置玩家动作
        actionManager.setAction(FakePlayerAction.STONECUTTING, new StonecuttingData(itemStack.getItem(), button));
        // 调用父类方法返还物品
        super.onClosed(player);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public int from() {
        return 1;
    }

    @Override
    public int to() {
        return 1;
    }
}
