package boat.carpetorgaddition.mixin.rule.channelingignoreweather;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public class WorldMixin {
    @WrapOperation(method = "isRaining", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getRainLevel(F)F"))
    private float raining(Level instance, @SuppressWarnings("NameDoesntMatchTargetClass") float value, Operation<Float> original) {
        if (CarpetOrgAdditionSettings.CHANNELING_IGNORE_CONDITIONS.value().isIgnoreWeather() && CarpetOrgAdditionSettings.USE_CHANNELING_TRIDENT.orElse(false)) {
            return 1F;
        }
        return original.call(instance, value);
    }

    @WrapOperation(method = "isThundering", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getThunderLevel(F)F"))
    private float thundering(Level instance, @SuppressWarnings("NameDoesntMatchTargetClass") float value, Operation<Float> original) {
        if (CarpetOrgAdditionSettings.CHANNELING_IGNORE_CONDITIONS.value().isIgnoreWeather() && CarpetOrgAdditionSettings.USE_CHANNELING_TRIDENT.orElse(false)) {
            return 1F;
        }
        return original.call(instance, value);
    }

    @Inject(method = "canHaveWeather", at = @At("HEAD"), cancellable = true)
    private void ignoreSky(CallbackInfoReturnable<Boolean> cir) {
        if (CarpetOrgAdditionSettings.CHANNELING_IGNORE_CONDITIONS.value().isIgnoreSky() && CarpetOrgAdditionSettings.USE_CHANNELING_TRIDENT.orElse(false)) {
            cir.setReturnValue(true);
        }
    }
}
