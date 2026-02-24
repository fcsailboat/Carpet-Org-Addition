package boat.carpetorgaddition.wheel.screen;

import boat.carpetorgaddition.periodic.FakePlayerComponentCoordinator;
import boat.carpetorgaddition.periodic.PlayerComponentCoordinator;
import boat.carpetorgaddition.periodic.fakeplayer.action.FakePlayerActionManager;
import boat.carpetorgaddition.periodic.fakeplayer.action.StonecuttingAction;
import boat.carpetorgaddition.wheel.predicate.ItemStackPredicate;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

public class StonecutterSetRecipeScreenHandler extends StonecutterMenu implements UnavailableSlotSyncInterface {
    private final EntityPlayerMPFake fakePlayer;

    public StonecutterSetRecipeScreenHandler(
            int syncId,
            Inventory playerInventory,
            ContainerLevelAccess screenHandlerContext,
            EntityPlayerMPFake fakePlayer
    ) {
        super(syncId, playerInventory, screenHandlerContext);
        this.fakePlayer = fakePlayer;
    }

    @Override
    public void clicked(int slotIndex, int button, @NonNull ContainerInput input, @NonNull Player player) {
        // 不能单击输出槽位
        if (slotIndex == 1) {
            return;
        }
        super.clicked(slotIndex, button, input, player);
    }

    @Override
    public void removed(@NonNull Player player) {
        ItemStack itemStack = this.container.getItem(0);
        if (itemStack.isEmpty()) {
            return;
        }
        // 获取按钮索引
        int button = this.getSelectedRecipeIndex();
        if (button != -1) {
            FakePlayerComponentCoordinator coordinator = PlayerComponentCoordinator.getCoordinator(this.fakePlayer);
            FakePlayerActionManager actionManager = coordinator.getFakePlayerActionManager();
            // 设置玩家动作
            actionManager.setAction(new StonecuttingAction(this.fakePlayer, new ItemStackPredicate(itemStack.getItem()), button));
        }
        // 调用父类方法返还物品
        super.removed(player);
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
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
