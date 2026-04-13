package boat.carpetorgaddition.mixin.rule.cceupdatesuppression;

import boat.carpetorgaddition.rule.RuleUtils;
import boat.carpetorgaddition.util.InventoryUtils;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.ShulkerBoxDispenseBehavior;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ShulkerBoxDispenseBehavior.class)
public class BlockPlacementDispenserBehaviorMixin {
    // 更新抑制潜影盒在被发射器放置时移除自定义名称
    @WrapOperation(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BlockItem;place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult dispenseSilently(BlockItem instance, BlockPlaceContext placeContext, Operation<InteractionResult> original, @Local(argsOnly = true, name = "dispensed") ItemStack dispensed) {
        Component component = dispensed.get(DataComponents.CUSTOM_NAME);
        if (component != null && InventoryUtils.isShulkerBoxItem(dispensed) && RuleUtils.canUpdateSuppression(component.getString())) {
            dispensed.remove(DataComponents.CUSTOM_NAME);
            InteractionResult result = original.call(instance, placeContext);
            dispensed.set(DataComponents.CUSTOM_NAME, component);
            return result;
        } else {
            return original.call(instance, placeContext);
        }
    }
}
