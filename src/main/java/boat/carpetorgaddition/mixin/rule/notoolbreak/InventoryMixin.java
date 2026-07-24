package boat.carpetorgaddition.mixin.rule.notoolbreak;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public class InventoryMixin {
    @Inject(method = "getSelectedItem", at = @At("HEAD"), cancellable = true)
    private void getSelectedItem(CallbackInfoReturnable<ItemStack> cir) {
        if (CarpetOrgAdditionSettings.USE_TOOL_ITEM_NO_BREAK.orElse(false)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
