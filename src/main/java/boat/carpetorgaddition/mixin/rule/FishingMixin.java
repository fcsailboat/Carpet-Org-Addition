package boat.carpetorgaddition.mixin.rule;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 禁用钓鱼开放水域检测
@Mixin(FishingHook.class)
public class FishingMixin {
    @Inject(method = "calculateOpenWater", at = @At("HEAD"), cancellable = true)
    private void isOpenOrWaterAround(BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
        if (CarpetOrgAdditionSettings.DISABLE_OPEN_OR_WATER_DETECTION.value()) {
            cir.setReturnValue(true);
        }
    }
}
