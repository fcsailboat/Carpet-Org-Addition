package org.carpetorgaddition.mixin.rule;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.WetSpongeBlock;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.rule.value.WetSpongeImmediatelyDry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WetSpongeBlock.class)
public class WetSpongeBlockMixin {
    // 湿海绵立即干燥
    @Inject(method = "onBlockAdded", at = @At(value = "HEAD"), cancellable = true)
    private void dry(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify, CallbackInfo ci) {
        switch (CarpetOrgAdditionSettings.wetSpongeImmediatelyDry) {
            case DISABLE:
                break;
            case ARID:
                if (world.getBiome(pos).value().hasPrecipitation()) {
                    break;
                }
            case FALSE:
                if (!world.getDimension().ultrawarm()
                        && CarpetOrgAdditionSettings.wetSpongeImmediatelyDry != WetSpongeImmediatelyDry.ARID) {
                    break;
                }
            case ALL:
                world.setBlockState(pos, Blocks.SPONGE.getDefaultState(), Block.NOTIFY_ALL);
                world.syncWorldEvent(WorldEvents.WET_SPONGE_DRIES_OUT, pos, 0);
                world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS,
                        1.0f, (1.0f + world.getRandom().nextFloat() * 0.2f) * 0.7f);
        }
        ci.cancel();
    }
}
