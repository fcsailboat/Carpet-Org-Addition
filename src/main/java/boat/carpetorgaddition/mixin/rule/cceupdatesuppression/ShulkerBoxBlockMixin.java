package boat.carpetorgaddition.mixin.rule.cceupdatesuppression;

import boat.carpetorgaddition.exception.CCEUpdateSuppressException;
import boat.carpetorgaddition.rule.RuleUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShulkerBoxBlock.class)
public class ShulkerBoxBlockMixin {
    // CCE更新抑制器
    @Inject(method = "getAnalogOutputSignal", at = @At("HEAD"))
    private void getComparatorOutput(BlockState state, Level level, BlockPos pos, Direction direction, CallbackInfoReturnable<Integer> cir) {
        // 不要在客户端抛出异常，这可能导致客户端游戏崩溃
        if (level.isClientSide()) {
            return;
        }
        if (RuleUtils.canUpdateSuppression(getBlockName(level, pos))) {
            throw new CCEUpdateSuppressException(pos, "CCE Update Suppress triggered on " + ServerUtils.toWorldPosString(level, pos));
        }
    }

    @Unique
    @Nullable
    private String getBlockName(Level world, BlockPos blockPos) {
        if (world.getBlockEntity(blockPos) instanceof ShulkerBoxBlockEntity shulkerBoxBlockEntity) {
            return shulkerBoxBlockEntity.getDisplayName().getString();
        }
        return null;
    }

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void onUse(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        // 提示玩家不能打开用于更新抑制的潜影盒
        if (RuleUtils.canUpdateSuppression(getBlockName(level, pos))) {
            MessageUtils.sendMessageToHud(player, LocalizationKeys.Rule.Message.CCE_UPDATE_SUPPRESSION.translate());
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}
