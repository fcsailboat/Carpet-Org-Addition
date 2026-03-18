package boat.carpetorgaddition.mixin.util;

import boat.carpetorgaddition.wheel.screen.AbstractPlayerInventoryScreenHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractContainerMenu.class)
public class ScreenHandlerMixin {
    /**
     * 修复如果被合并的两个物品是同一个对象时，可能发生的物品复制问题
     *
     * @see AbstractPlayerInventoryScreenHandler#quickMoveStack(Player, int)
     */
    @WrapOperation(method = "moveItemStackTo", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isSameItemSameComponents(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean isEqual(ItemStack stack, ItemStack otherStack, Operation<Boolean> original) {
        if (AbstractPlayerInventoryScreenHandler.QUICK_MOVING_ITEM.orElse(false) && stack == otherStack) {
            return false;
        }
        return original.call(stack, otherStack);
    }
}
