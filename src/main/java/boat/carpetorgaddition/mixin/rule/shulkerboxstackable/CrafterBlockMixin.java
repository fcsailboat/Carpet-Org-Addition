package boat.carpetorgaddition.mixin.rule.shulkerboxstackable;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CrafterBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CrafterBlock.class)
public class CrafterBlockMixin {
    @WrapOperation(method = "dispenseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack transfer(Container from, Container to, ItemStack stack, Direction side, Operation<ItemStack> original) {
        boolean changed = CarpetOrgAdditionSettings.SHULKER_BOX_STACK_COUNT_CHANGED.get();
        try {
            CarpetOrgAdditionSettings.SHULKER_BOX_STACK_COUNT_CHANGED.set(false);
            return original.call(from, to, stack, side);
        } finally {
            CarpetOrgAdditionSettings.SHULKER_BOX_STACK_COUNT_CHANGED.set(changed);
        }
    }
}
