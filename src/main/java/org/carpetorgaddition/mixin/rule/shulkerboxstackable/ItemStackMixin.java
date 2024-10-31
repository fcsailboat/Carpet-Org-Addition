package org.carpetorgaddition.mixin.rule.shulkerboxstackable;

import net.minecraft.item.ItemStack;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.util.InventoryUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @Unique
    private final ItemStack thisStack = (ItemStack) (Object) this;

    @Inject(method = "getMaxCount", at = @At("HEAD"), cancellable = true)
    private void getMaxCount(CallbackInfoReturnable<Integer> cir) {
        if (this.shulkerBoxStackableEnabled() && CarpetOrgAdditionSettings.shulkerBoxStackCountChanged.get()) {
            cir.setReturnValue(64);
        }
    }

    @Inject(method = "capCount", at = @At("HEAD"), cancellable = true)
    private void capCount(int maxCount, CallbackInfo ci) {
        if (this.shulkerBoxStackableEnabled() && !CarpetOrgAdditionSettings.shulkerBoxStackCountChanged.get()) {
            ci.cancel();
        }
    }

    // 规则是否已启用，并且当前物品是空潜影盒
    @Unique
    private boolean shulkerBoxStackableEnabled() {
        return CarpetOrgAdditionSettings.shulkerBoxStackable
                && InventoryUtils.isShulkerBoxItem(thisStack)
                && InventoryUtils.isEmptyShulkerBox(thisStack);
    }
}
