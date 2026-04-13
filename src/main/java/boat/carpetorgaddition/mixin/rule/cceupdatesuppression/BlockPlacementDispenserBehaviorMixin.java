package boat.carpetorgaddition.mixin.rule.cceupdatesuppression;

import boat.carpetorgaddition.rule.RuleUtils;
import boat.carpetorgaddition.util.InventoryUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.ShulkerBoxDispenseBehavior;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShulkerBoxDispenseBehavior.class)
public class BlockPlacementDispenserBehaviorMixin {
    // 更新抑制潜影盒在被发射器放置时移除自定义名称
    @Inject(method = "execute", at = @At("HEAD"))
    private void dispenseSilently(BlockSource source, ItemStack dispensed, CallbackInfoReturnable<ItemStack> cir) {
        // 发射器放置潜影盒时会直接清除整组物品的名称，而不是只清除放置的那个，需要修复吗？
        if (InventoryUtils.isShulkerBoxItem(dispensed) && RuleUtils.canUpdateSuppression(dispensed.getHoverName().getString())) {
            dispensed.remove(DataComponents.CUSTOM_NAME);
        }
    }
}
