package boat.carpetorgaddition.mixin.rule;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//猪灵快速交易
@Mixin(PiglinAi.class)
public abstract class PiglinBrainMixin {
    @Inject(method = "admireGoldItem", at = @At("HEAD"), cancellable = true)
    private static void setAdmiringItem(LivingEntity body, CallbackInfo ci) {
        Long time = CarpetOrgAdditionSettings.CUSTOM_PIGLIN_BARTERING_TIME.value();
        if (time != -1) {
            body.getBrain().setMemoryWithExpiry(MemoryModuleType.ADMIRING_ITEM, true, time);
            ci.cancel();
        }
    }
}
