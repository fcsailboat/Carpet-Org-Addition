package org.carpetorgaddition.mixin.rule;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.SmithingScreenHandler;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SmithingScreenHandler.class)
public abstract class SmithingScreenHandlerMixin {
    @Shadow
    protected abstract List<ItemStack> getInputStacks();

    // 可重复使用的锻造模板
    @Inject(method = "decrementStack", at = @At("HEAD"), cancellable = true)
    private void decrement(int slot, CallbackInfo ci) {
        ItemStack itemStack = this.getInputStacks().get(slot);
        if (slot == 0) {
            switch (CarpetOrgAdditionSettings.reusableSmithingTemplate) {
                case FALSE -> {
                }
                case UPGRADE -> {
                    if (itemStack.isOf(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)) {
                        ci.cancel();
                    }
                }
                case TRUE -> ci.cancel();
            }
        }
    }
}