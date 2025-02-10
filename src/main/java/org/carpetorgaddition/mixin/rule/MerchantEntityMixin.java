package org.carpetorgaddition.mixin.rule;

import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MerchantEntity.class)
public abstract class MerchantEntityMixin {
    @Shadow
    public abstract @Nullable PlayerEntity getCustomer();

    @Inject(method = "canInteract", at = @At("HEAD"), cancellable = true)
    private void canInteract(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        if (CarpetOrgAdditionSettings.villagerVoidTrading) {
            cir.setReturnValue(this.getCustomer() == player);
        }
    }
}
