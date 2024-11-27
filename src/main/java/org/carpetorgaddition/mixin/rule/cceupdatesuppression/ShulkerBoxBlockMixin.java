package org.carpetorgaddition.mixin.rule.cceupdatesuppression;

import net.minecraft.block.BlockState;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.carpetorgaddition.exception.CCEUpdateSuppressException;
import org.carpetorgaddition.rule.RuleUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.WorldUtils;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShulkerBoxBlock.class)
public class ShulkerBoxBlockMixin {
    // CCE更新抑制器
    @Inject(method = "getComparatorOutput", at = @At("HEAD"))
    private void getComparatorOutput(BlockState state, World world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        // 不要在客户端抛出异常，这可能导致客户端游戏崩溃
        if (world.isClient) {
            return;
        }
        if (RuleUtils.canUpdateSuppression(getBlockName(world, pos))) {
            throw new CCEUpdateSuppressException(pos, "CCE Update Suppress triggered on " + WorldUtils.toWorldPosString(world, pos));
        }
    }

    @Unique
    @Nullable
    private String getBlockName(World world, BlockPos blockPos) {
        if (world.getBlockEntity(blockPos) instanceof ShulkerBoxBlockEntity shulkerBoxBlockEntity) {
            return shulkerBoxBlockEntity.getDisplayName().getString();
        }
        return null;
    }

    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit,
                       CallbackInfoReturnable<ActionResult> cir) {
        // 提示玩家不能打开用于更新抑制的潜影盒
        if (RuleUtils.canUpdateSuppression(getBlockName(world, pos))) {
            MessageUtils.sendMessageToHud(player, TextUtils.translate("carpet.rule.message.CCEUpdateSuppression"));
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }
}
