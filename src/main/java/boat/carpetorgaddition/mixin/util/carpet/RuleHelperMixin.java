package boat.carpetorgaddition.mixin.util.carpet;

import boat.carpetorgaddition.rule.BuiltRule;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.RuleHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RuleHelper.class, remap = false)
public class RuleHelperMixin {
    @Inject(method = "translatedName", at = @At("HEAD"), cancellable = true)
    private static void translatedName(CarpetRule<?> rule, CallbackInfoReturnable<String> cir) {
        if (rule instanceof BuiltRule<?> builtRule) {
            String displayName = builtRule.getDisplayName();
            if (displayName.isEmpty()) {
                return;
            }
            cir.setReturnValue(displayName);
        }
    }

    @Inject(method = "translatedDescription", at = @At("HEAD"), cancellable = true)
    private static void translatedDescription(CarpetRule<?> rule, CallbackInfoReturnable<String> cir) {
        if (rule instanceof BuiltRule<?> builtRule) {
            String displayDesc = builtRule.getDisplayDesc();
            if (displayDesc.isEmpty()) {
                return;
            }
            cir.setReturnValue(displayDesc);
        }
    }
}
