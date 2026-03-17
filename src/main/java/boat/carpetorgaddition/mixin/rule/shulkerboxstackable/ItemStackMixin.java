package boat.carpetorgaddition.mixin.rule.shulkerboxstackable;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.rule.RuleUtils;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @Unique
    private final ItemStack self = (ItemStack) (Object) this;

    @Inject(method = "limitSize", at = @At("HEAD"), cancellable = true)
    private void capCount(int maxCount, CallbackInfo ci) {
        if (!CarpetOrgAdditionSettings.SHULKER_BOX_STACK_COUNT_CHANGED.orElse(true) && RuleUtils.shulkerBoxStackableEnabled(this.self)) {
            ci.cancel();
        }
    }
}
