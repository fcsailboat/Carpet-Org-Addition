package boat.carpetorgaddition.mixin.rule.channelingignoreweather;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndLightGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockAndLightGetter.class)
public interface BlockAndTintGetterMixin {
    @Inject(method = "canSeeSky", at = @At("HEAD"), cancellable = true)
    private void canSeeSky(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (CarpetOrgAdditionSettings.channelingIgnoreConditions.value().isIgnoreSky() && CarpetOrgAdditionSettings.USE_CHANNELING_TRIDENT.orElse(false)) {
            cir.setReturnValue(true);
        }
    }
}
