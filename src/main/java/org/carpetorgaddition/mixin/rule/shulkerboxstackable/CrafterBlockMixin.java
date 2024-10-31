package org.carpetorgaddition.mixin.rule.shulkerboxstackable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.CrafterBlock;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CrafterBlock.class)
public class CrafterBlockMixin {
    @WrapOperation(method = "transferOrSpawnStack", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/HopperBlockEntity;transfer(Lnet/minecraft/inventory/Inventory;Lnet/minecraft/inventory/Inventory;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/math/Direction;)Lnet/minecraft/item/ItemStack;"))
    private ItemStack transfer(Inventory from, Inventory to, ItemStack stack, Direction side, Operation<ItemStack> original) {
        try {
            CarpetOrgAdditionSettings.shulkerBoxStackCountChanged.set(false);
            return original.call(from, to, stack, side);
        } finally {
            CarpetOrgAdditionSettings.shulkerBoxStackCountChanged.set(true);
        }
    }
}
