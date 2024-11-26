package org.carpetorgaddition.mixin.rule.canminespawner;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.Spawner;
import net.minecraft.item.BlockItem;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockItem.class)
public class BlockItemMixin {
    @WrapOperation(method = "writeNbtToBlockEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/BlockEntityType;canPotentiallyExecuteCommands()Z"))
    private static boolean writeNbtToBlockEntity(BlockEntityType<?> instance, Operation<Boolean> original, @Local BlockEntity blockEntity) {
        if (CarpetOrgAdditionSettings.canMineSpawner && blockEntity instanceof Spawner) {
            return false;
        }
        return original.call(instance);
    }
}
