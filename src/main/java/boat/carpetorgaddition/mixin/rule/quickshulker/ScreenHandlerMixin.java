package boat.carpetorgaddition.mixin.rule.quickshulker;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.periodic.fakeplayer.FakePlayerUtils;
import boat.carpetorgaddition.util.InventoryUtils;
import boat.carpetorgaddition.util.MathUtils;
import boat.carpetorgaddition.util.PlayerUtils;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class ScreenHandlerMixin {
    @Shadow
    @Final
    public NonNullList<Slot> slots;

    @Shadow
    public abstract Slot getSlot(int index);

    @Shadow
    public abstract ItemStack getCarried();

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void onSlotClick(int slotIndex, int buttonNum, ContainerInput containerInput, Player player, CallbackInfo ci) {
        if (CarpetOrgAdditionSettings.QUICK_SHULKER.value() && MathUtils.isInRange(0, this.slots.size(), slotIndex) && containerInput == ContainerInput.PICKUP && buttonNum == FakePlayerUtils.PICKUP_RIGHT_CLICK) {
            ItemStack stack = this.getSlot(slotIndex).getItem();
            if (this.canOpenShulker() && InventoryUtils.isOperableSulkerBox(stack) && this.getCarried().isEmpty()) {
                // 创造模式物品栏是一个客户端屏幕，因此点击潜影盒不会打开物品栏
                if (player instanceof ServerPlayer) {
                    PlayerUtils.openShulkerScreenHandler((ServerPlayer) player, stack);
                }
                ci.cancel();
            }
        }
    }

    /**
     * @return 当前屏幕是否可以打开潜影盒
     */
    @Unique
    protected boolean canOpenShulker() {
        return true;
    }
}
