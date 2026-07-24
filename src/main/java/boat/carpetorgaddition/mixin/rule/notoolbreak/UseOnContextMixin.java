package boat.carpetorgaddition.mixin.rule.notoolbreak;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(UseOnContext.class)
public class UseOnContextMixin {
    @Inject(method = "getItemInHand", at = @At("HEAD"), cancellable = true)
    private void getItemInHand(CallbackInfoReturnable<ItemStack> cir) {
        if (CarpetOrgAdditionSettings.USE_TOOL_ITEM_NO_BREAK.orElse(false)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
