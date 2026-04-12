package boat.carpetorgaddition.mixin.rule;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ObserverBlock.class)
public abstract class ObserverBlockMixin extends DirectionalBlock {
    @Shadow
    protected abstract void startSignal(LevelReader level, ScheduledTickAccess ticks, BlockPos pos);

    private ObserverBlockMixin(Properties settings) {
        super(settings);
    }

    // 可激活侦测器，打火石右键激活
    @Override
    protected @NonNull InteractionResult useItemOn(@NonNull ItemStack stack, @NonNull BlockState state, @NonNull Level world, @NonNull BlockPos pos, @NonNull Player player, @NonNull InteractionHand hand, @NonNull BlockHitResult hit) {
        if (CarpetOrgAdditionSettings.CAN_ACTIVATES_OBSERVER.value()) {
            ItemStack itemStack = player.getItemInHand(hand);
            if (itemStack.is(Items.FLINT_AND_STEEL) && !player.isShiftKeyDown()) {
                this.startSignal(world, world, pos);
                stack.hurtAndBreak(1, player, hand);
                world.playSound(player, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1, 1);
                player.awardStat(Stats.ITEM_USED.get(itemStack.getItem()));
                return InteractionResult.SUCCESS;
            }
        }
        return super.useItemOn(stack, state, world, pos, player, hand, hit);
    }
}
