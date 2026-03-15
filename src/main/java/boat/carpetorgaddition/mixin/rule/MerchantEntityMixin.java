package boat.carpetorgaddition.mixin.rule;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractVillager.class)
public abstract class MerchantEntityMixin {
    @Shadow
    public abstract @Nullable Player getTradingPlayer();

    @Inject(method = "stillValid", at = @At("HEAD"), cancellable = true)
    private void canInteract(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (CarpetOrgAdditionSettings.VILLAGER_VOID_TRADING.value()) {
            cir.setReturnValue(this.getTradingPlayer() == player);
        }
    }
}
