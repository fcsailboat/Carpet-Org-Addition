package org.carpetorgaddition.mixin.rule.shulkerboxstackable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(HopperBlockEntity.class)
public class HopperBlockEntityMixin {
    // 漏斗
    @WrapOperation(method = "insertAndExtract", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/HopperBlockEntity;insert(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/HopperBlockEntity;)Z"))
    private static boolean insert(World world, BlockPos pos, HopperBlockEntity blockEntity, Operation<Boolean> original) {
        try {
            if (CarpetOrgAdditionSettings.shulkerBoxStackable) {
                CarpetOrgAddition.shulkerBoxStackCountChanged.set(false);
            }
            return original.call(world, pos, blockEntity);
        } finally {
            CarpetOrgAddition.shulkerBoxStackCountChanged.set(CarpetOrgAdditionSettings.shulkerBoxStackable);
        }
    }
}
