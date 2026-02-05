package org.carpetorgaddition.mixin.rule;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.block.AbstractBlock.AbstractBlockState;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.carpetorgaddition.wheel.BlockHardnessModifiers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

// 优先保证Carpet AMS Addition的自定义方块硬度可用
@Mixin(value = AbstractBlockState.class, priority = 999)
public abstract class AbstractBlockStateMixin {
    @Shadow
    public abstract Block getBlock();

    /**
     * 修改硬度的基岩不会被推动
     * {@link PistonBlockMixin}
     */
    @ModifyReturnValue(method = "getHardness", at = @At("RETURN"))
    public float getBlockHardness(float hardness, @Local(argsOnly = true) BlockView world, @Local(argsOnly = true) BlockPos pos) {
        return BlockHardnessModifiers.getHardness(this.getBlock(), world, pos, hardness);
    }
}
