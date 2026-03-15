package boat.carpetorgaddition.mixin.rule;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 注视末影人眼睛时不会激怒末影人
@Mixin(EnderMan.class)
public class EndermanEntityMixin {
    @Inject(method = "isBeingStaredBy", at = @At("HEAD"), cancellable = true)
    private void isPlayerStaring(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (CarpetOrgAdditionSettings.STARING_ENDERMAN_NOT_ANGRY.value()) {
            cir.setReturnValue(false);
        }
    }
}
