package boat.carpetorgaddition.mixin.rule;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceBlockEntity.class)
public class AbstractFurnaceBlockEntityMixin {
    @Inject(method = "createExperience", at = @At("HEAD"), cancellable = true)
    private static void dropExperience(CallbackInfo ci) {
        if (CarpetOrgAdditionSettings.DISABLE_FURNACE_DROP_EXPERIENCE.value()) {
            ci.cancel();
        }
    }
}
