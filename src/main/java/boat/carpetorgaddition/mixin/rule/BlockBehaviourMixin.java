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
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.class)
public class BlockBehaviourMixin {
    @Unique
    private final BlockBehaviour self = (BlockBehaviour) (Object) this;

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void useItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (CarpetOrgAdditionSettings.CAN_ACTIVATES_OBSERVER.value() && this.self instanceof ObserverBlock observer) {
            if (player.isShiftKeyDown()) {
                return;
            }
            /*
             * 应该检测侦测器的方块状态吗？虽然已经激活的侦测器不应该再次右键激活，
             * 但这如果该侦测器没有计划刻，例如通过粘贴投影原理图放置的激活的侦测器，
             * 则可以通过右键来添加计划刻
             */
            if (itemStack.is(Items.FLINT_AND_STEEL)) {
                itemStack.hurtAndBreak(1, player, hand);
                level.playSound(player, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1, 1);
                observer.startSignal(level, level, pos);
                player.awardStat(Stats.ITEM_USED.get(itemStack.getItem()));
                cir.setReturnValue(InteractionResult.SUCCESS);
            } else if (itemStack.is(Items.FIRE_CHARGE)) {
                itemStack.consume(1, player);
                level.playSound(player, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1, 1);
                observer.startSignal(level, level, pos);
                player.awardStat(Stats.ITEM_USED.get(itemStack.getItem()));
                cir.setReturnValue(InteractionResult.SUCCESS);
            }
        }
    }
}
