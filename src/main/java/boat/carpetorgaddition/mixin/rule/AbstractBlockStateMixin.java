package boat.carpetorgaddition.mixin.rule;

import boat.carpetorgaddition.wheel.misc.BlockHardnessModifiers;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

// 优先保证Carpet AMS Addition的自定义方块硬度可用
@Mixin(value = BlockStateBase.class, priority = 999)
public abstract class AbstractBlockStateMixin {
    @Shadow
    public abstract Block getBlock();

    /**
     * 修改硬度的基岩不会被推动
     * {@link PistonBlockMixin}
     */
    @ModifyReturnValue(method = "getDestroySpeed", at = @At("RETURN"))
    public float getBlockHardness(float hardness, @Local(argsOnly = true, name = "level") BlockGetter level, @Local(argsOnly = true, name = "pos") BlockPos pos) {
        return BlockHardnessModifiers.getHardness(this.getBlock(), level, pos, hardness);
    }
}
