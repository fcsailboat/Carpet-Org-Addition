package boat.carpetorgaddition.mixin.rule;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import net.minecraft.world.entity.ambient.Bat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 禁止蝙蝠生成
@Mixin(Bat.class)
public class BatEntityMixin {
    @Inject(method = "checkBatSpawnRules", at = @At("HEAD"), cancellable = true)
    private static void canSpawn(CallbackInfoReturnable<Boolean> cir) {
        if (CarpetOrgAdditionSettings.DISABLE_BAT_CAN_SPAWN.value()) {
            cir.setReturnValue(false);
        }
    }
}
