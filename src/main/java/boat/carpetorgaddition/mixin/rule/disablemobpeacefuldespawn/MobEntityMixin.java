package boat.carpetorgaddition.mixin.rule.disablemobpeacefuldespawn;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Mob.class)
public class MobEntityMixin {
    @Unique
    private final Mob thisMob = (Mob) (Object) this;

    // 禁止特定生物在和平模式下被清除
    @WrapOperation(method = "checkDespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;isAllowedInPeaceful()Z"))
    private boolean isDisallowedInPeaceful(EntityType<?> mob, Operation<Boolean> original) {
        if (CarpetOrgAdditionSettings.DISABLE_MOB_PEACEFUL_DESPAWN.value() && (thisMob.isPersistenceRequired() || thisMob.requiresCustomPersistence())) {
            return true;
        }
        return original.call(mob);
    }
}
