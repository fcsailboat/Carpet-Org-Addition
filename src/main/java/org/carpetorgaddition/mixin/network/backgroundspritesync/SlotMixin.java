package org.carpetorgaddition.mixin.network.backgroundspritesync;

import com.mojang.datafixers.util.Pair;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import org.carpetorgaddition.util.screen.BackgroundSpriteSyncSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(Slot.class)
public class SlotMixin implements BackgroundSpriteSyncSlot {
    @Unique
    private Identifier identifier;

    @Inject(method = "getBackgroundSprite", at = @At("HEAD"), cancellable = true)
    private void getBackgroundSprite(CallbackInfoReturnable<Pair<Identifier, Identifier>> cir) {
        Pair<Identifier, Identifier> pair = cir.getReturnValue();
        if (pair == null && this.identifier != null) {
            cir.setReturnValue(Pair.of(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, identifier));
        }
    }

    @Override
    public void setIdentifier(Identifier identifier) {
        this.identifier = identifier;
    }
}
