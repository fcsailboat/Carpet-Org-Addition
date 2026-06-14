package boat.carpetorgaddition.mixin.network;

import boat.carpetorgaddition.wheel.screen.AbstractPlayerInventoryScreenHandler;
import boat.carpetorgaddition.wheel.screen.WithButtonPlayerInventoryScreenHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 在父类中实现接口，这样就可以方便的注入{@link AbstractContainerMenu#clicked(int, int, ContainerInput, Player)}方法
 */
@Mixin(ChestMenu.class)
public class ChestMenuMixin extends ScreenHandlerMixin {
    @Environment(EnvType.CLIENT)
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void quickMove(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
        ChestMenu menu = (ChestMenu) (Object) this;
        if (this.carpet_Org_Addition$isWithButton()) {
            cir.setReturnValue(WithButtonPlayerInventoryScreenHandler.quickMoveStack(menu, slotIndex));
        } else if (this.carpet_Org_Addition$isOldPlayerInventoryScreen()) {
            cir.setReturnValue(AbstractPlayerInventoryScreenHandler.quickMove(menu, slotIndex));
        }
    }
}
