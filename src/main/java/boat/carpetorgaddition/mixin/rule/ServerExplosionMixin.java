package boat.carpetorgaddition.mixin.rule;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;
import net.minecraft.world.level.ServerExplosion;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerExplosion.class)
public class ServerExplosionMixin {
    @Shadow
    @Final
    private @Nullable Entity source;

    @Inject(method = "canTriggerBlocks", at = @At("HEAD"), cancellable = true)
    private void disableTriggerEffect(CallbackInfoReturnable<Boolean> cir) {
        if (CarpetOrgAdditionSettings.DISABLE_WIND_CHARGE_EFFECT.value() && this.source instanceof WindCharge charge && charge.getOwner() instanceof Player) {
            cir.setReturnValue(false);
        }
    }
}
