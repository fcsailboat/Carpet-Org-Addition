package boat.carpetorgaddition.mixin.rule;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.Boat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(Entity.class)
public abstract class EntityMixin {
    @Unique
    private final Entity thisEntity = (Entity) (Object) this;

    // 登山船
    @Inject(method = "maxUpStep", at = @At("HEAD"), cancellable = true)
    private void getStepHeight(CallbackInfoReturnable<Float> cir) {
        if (CarpetOrgAdditionSettings.CLIMBING_BOAT.value()
            && thisEntity instanceof Boat boatEntity
            && boatEntity.getControllingPassenger() instanceof Player) {
            cir.setReturnValue(1.0F);
        }
    }
}
