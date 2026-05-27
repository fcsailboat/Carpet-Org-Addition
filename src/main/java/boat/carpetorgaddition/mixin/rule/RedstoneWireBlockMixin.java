package boat.carpetorgaddition.mixin.rule;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 红石线不会连接到打开的活板门上的红石线
@Mixin(RedStoneWireBlock.class)
public class RedstoneWireBlockMixin {
    // 红石线不会连接到打开的活板门上的红石线
    @WrapOperation(method = "getConnectingSide(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Z)Lnet/minecraft/world/level/block/state/properties/RedstoneSide;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;"))
    private Block getBlock(BlockState instance, Operation<Block> original) {
        if (CarpetOrgAdditionSettings.SIMPLE_UPDATE_SKIPPER.value()) {
            return null;
        }
        return original.call(instance);
    }

    // 防止红石线掉落
    @Inject(method = "updateShape", at = @At("HEAD"), cancellable = true)
    private void preventDrop(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random, CallbackInfoReturnable<BlockState> cir) {
        if (CarpetOrgAdditionSettings.SIMPLE_UPDATE_SKIPPER.value() && directionToNeighbour == Direction.DOWN) {
            cir.setReturnValue(state);
        }
    }
}
