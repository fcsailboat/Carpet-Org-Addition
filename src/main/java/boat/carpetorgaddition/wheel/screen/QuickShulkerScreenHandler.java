package boat.carpetorgaddition.wheel.screen;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.periodic.fakeplayer.FakePlayerUtils;
import boat.carpetorgaddition.util.MathUtils;
import boat.carpetorgaddition.wheel.inventory.ContainerComponentInventory;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.function.Predicate;

public class QuickShulkerScreenHandler extends ShulkerBoxMenu implements UnavailableSlotSyncInterface {
    private final int shulkerSlotIndex;
    private final ContainerComponentInventory inventory;
    private final ServerPlayer player;
    /**
     * 是否可以继续使用快捷潜影盒
     */
    private final Predicate<Player> predicate;
    private final ItemStack shulkerBox;

    public QuickShulkerScreenHandler(
            int syncId,
            Inventory playerInventory,
            ContainerComponentInventory inventory,
            ServerPlayer player,
            Predicate<Player> predicate,
            ItemStack shulkerBox
    ) {
        super(syncId, playerInventory, inventory);
        this.inventory = inventory;
        this.player = player;
        this.predicate = predicate;
        this.shulkerBox = shulkerBox;
        if (canUseQuickShulker()) {
            for (Slot slot : this.slots) {
                if (slot.getItem() == shulkerBox) {
                    this.shulkerSlotIndex = slot.index;
                    return;
                }
            }
            this.shulkerSlotIndex = -1;
        } else {
            throw new IllegalStateException("Quick shulker box not enabled");
        }
    }

    @Override
    public void clicked(int slotIndex, int button, @NonNull ContainerInput input, @NonNull Player player) {
        if (MathUtils.isInRange(this.from(), this.to(), slotIndex)) {
            if (button == FakePlayerUtils.PICKUP_RIGHT_CLICK) {
                ItemStack cursorStack = this.getCarried();
                // 光标物品是否可以放入潜影盒
                if (cursorStack.getItem().canFitInsideContainerItems()) {
                    ItemStack remaining = this.inventory.addItem(cursorStack);
                    this.setCarried(remaining);
                }
            }
            return;
        }
        super.clicked(slotIndex, button, input, player);
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        if (this.shulkerBox.isEmpty()) {
            return false;
        }
        return canUseQuickShulker() && this.shulkerBox.getCount() == 1 && this.predicate.test(player) && super.stillValid(player);
    }

    private boolean canUseQuickShulker() {
        return CarpetOrgAdditionSettings.QUICK_SHULKER.value() || this.player instanceof EntityPlayerMPFake;
    }

    @Override
    public void slotsChanged(@NonNull Container inventory) {
        super.slotsChanged(inventory);
    }

    @Override
    public int from() {
        return this.shulkerSlotIndex == -1 ? 0 : this.shulkerSlotIndex;
    }

    @Override
    public int to() {
        return this.shulkerSlotIndex;
    }

    public ItemStack getShulkerBox() {
        return this.shulkerBox;
    }

    public static boolean isOpenedShulkerBox(ServerPlayer player, ItemStack itemStack) {
        if (player.containerMenu instanceof QuickShulkerScreenHandler screenHandler) {
            return itemStack != screenHandler.getShulkerBox();
        }
        return false;
    }
}
