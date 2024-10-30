package org.carpetorgaddition.mixin.rule.shulkerboxstackable;

import net.minecraft.item.ItemStack;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.util.InventoryUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @Unique
    private final ItemStack thisStack = (ItemStack) (Object) this;

    @Inject(method = "getMaxCount", at = @At("HEAD"), cancellable = true)
    private void getMaxCount(CallbackInfoReturnable<Integer> cir) {
        if (CarpetOrgAdditionSettings.shulkerBoxStackable && InventoryUtils.isShulkerBoxItem(thisStack)) {
            if (CarpetOrgAddition.shulkerBoxStackCountChanged.get() && InventoryUtils.isEmptyShulkerBox(thisStack)) {
                cir.setReturnValue(64);
            }
        }
    }
}
