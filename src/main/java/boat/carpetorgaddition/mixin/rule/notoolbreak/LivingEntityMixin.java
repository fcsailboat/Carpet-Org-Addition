package boat.carpetorgaddition.mixin.rule.notoolbreak;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Unique
    private final LivingEntity self = (LivingEntity) (Object) this;

    @Inject(method = "getItemInHand", at = @At("HEAD"), cancellable = true)
    private void getItemInHand(InteractionHand hand, CallbackInfoReturnable<ItemStack> cir) {
        if (this.self instanceof Player && CarpetOrgAdditionSettings.USE_TOOL_ITEM_NO_BREAK.orElse(false)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }

    @Inject(method = "getMainHandItem", at = @At("HEAD"), cancellable = true)
    private void getMainHandItem(CallbackInfoReturnable<ItemStack> cir) {
        if (this.self instanceof Player && CarpetOrgAdditionSettings.USE_TOOL_ITEM_NO_BREAK.orElse(false)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
