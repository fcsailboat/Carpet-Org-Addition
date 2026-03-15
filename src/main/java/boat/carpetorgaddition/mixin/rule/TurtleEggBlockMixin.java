package boat.carpetorgaddition.mixin.rule;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TurtleEggBlock.class)
public abstract class TurtleEggBlockMixin extends Block {
    @Shadow
    @Final
    public static IntegerProperty EGGS;

    public TurtleEggBlockMixin(Properties settings) {
        super(settings);
    }

    //海龟蛋快速孵化
    @Inject(method = "shouldUpdateHatchLevel", at = @At("HEAD"), cancellable = true)
    private void progress(Level world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (CarpetOrgAdditionSettings.TURTLE_EGG_FAST_HATCH.value()) {
            cir.setReturnValue(true);
        }
    }

    // 海龟蛋快速采集
    @Inject(method = "playerDestroy", at = @At("HEAD"), cancellable = true)
    private void afterBreak(Level world, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity, ItemStack tool, CallbackInfo ci) {
        if (CarpetOrgAdditionSettings.TURTLE_EGG_FAST_MINE.value()) {
            for (int i = 0; i < state.getValue(EGGS); i++) {
                super.playerDestroy(world, player, pos, state, blockEntity, tool);
            }
            // 播放海龟蛋破坏音效
            world.playSound(null, pos, SoundEvents.TURTLE_EGG_BREAK, SoundSource.BLOCKS, 0.7F, 0.9F + world.getRandom().nextFloat() * 0.2F);
            ci.cancel();
        }
    }
}
