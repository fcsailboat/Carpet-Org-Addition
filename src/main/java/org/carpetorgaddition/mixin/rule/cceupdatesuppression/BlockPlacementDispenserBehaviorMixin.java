package org.carpetorgaddition.mixin.rule.cceupdatesuppression;

import net.minecraft.block.dispenser.BlockPlacementDispenserBehavior;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPointer;
import org.carpetorgaddition.rule.RuleUtils;
import org.carpetorgaddition.util.InventoryUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockPlacementDispenserBehavior.class)
public class BlockPlacementDispenserBehaviorMixin {
    // 更新抑制潜影盒在被发射器放置时移除自定义名称
    @Inject(method = "dispenseSilently", at = @At("HEAD"))
    private void dispenseSilently(BlockPointer pointer, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        if (InventoryUtils.isShulkerBoxItem(stack) && RuleUtils.canUpdateSuppression(stack.getName().getString())) {
            stack.remove(DataComponentTypes.CUSTOM_NAME);
        }
    }
}
