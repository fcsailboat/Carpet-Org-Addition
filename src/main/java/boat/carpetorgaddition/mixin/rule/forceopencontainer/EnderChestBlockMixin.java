package boat.carpetorgaddition.mixin.rule.forceopencontainer;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EnderChestBlock.class)
public class EnderChestBlockMixin {
    @WrapOperation(method = "useWithoutItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;isRedstoneConductor(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean forceOpenEnderChest(BlockState instance, BlockGetter blockGetter, BlockPos blockPos, Operation<Boolean> original) {
        if (CarpetOrgAdditionSettings.FORCE_OPEN_CONTAINER.value().canOpenChest()) {
            return false;
        }
        return original.call(instance, blockGetter, blockPos);
    }
}
