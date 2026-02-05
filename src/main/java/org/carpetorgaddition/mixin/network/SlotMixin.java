package org.carpetorgaddition.mixin.network;

import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import org.carpetorgaddition.wheel.screen.BackgroundSpriteSyncSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public class SlotMixin implements BackgroundSpriteSyncSlot {
    @Unique
    private Identifier identifier;

    @Inject(method = "getBackgroundSprite", at = @At("HEAD"), cancellable = true)
    private void getBackgroundSprite(CallbackInfoReturnable<Identifier> cir) {
        Identifier pair = cir.getReturnValue();
        if (pair == null && this.identifier != null) {
            cir.setReturnValue(identifier);
        }
    }

    @Override
    public void carpet_Org_Addition$setIdentifier(Identifier identifier) {
        this.identifier = identifier;
    }
}
