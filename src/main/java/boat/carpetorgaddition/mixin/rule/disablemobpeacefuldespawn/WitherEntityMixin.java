package boat.carpetorgaddition.mixin.rule.disablemobpeacefuldespawn;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WitherBoss.class)
public class WitherEntityMixin {
    @Unique
    private final WitherBoss thisWither = (WitherBoss) (Object) this;

    // 禁止特定生物在和平模式下被清除（凋灵）
    @WrapOperation(method = "checkDespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;isAllowedInPeaceful()Z"))
    private boolean isDisallowedInPeaceful(EntityType<?> instance, Operation<Boolean> original) {
        if (CarpetOrgAdditionSettings.DISABLE_MOB_PEACEFUL_DESPAWN.value() && (thisWither.isPersistenceRequired() || thisWither.requiresCustomPersistence())) {
            return true;
        }
        return original.call(instance);
    }
}
