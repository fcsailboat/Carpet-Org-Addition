package org.carpetorgaddition.mixin.rule;

import net.minecraft.block.BlockState;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.exception.CCEUpdateSuppressException;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.WorldUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;


@Mixin(ShulkerBoxBlock.class)
public class ShulkerBoxBlockMixin {
    // 强制开启潜影盒
    @Inject(method = "canOpen", at = @At(value = "HEAD"), cancellable = true)
    private static void canOpen(BlockState state, World world, BlockPos pos, ShulkerBoxBlockEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (CarpetOrgAdditionSettings.openShulkerBoxForcibly) {
            cir.setReturnValue(true);
        }
    }

    // CCE更新抑制器
    @Inject(method = "getComparatorOutput", at = @At("HEAD"))
    private void getComparatorOutput(BlockState state, World world, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        // 不要在客户端抛出异常，这可能导致客户端游戏崩溃
        if (world.isClient) {
            return;
        }
        if (canUpdateSuppression(world, pos)) {
            throw new CCEUpdateSuppressException(pos, "CCE Update Suppress triggered on " + WorldUtils.toWorldPosString(world, pos));
        }
    }

    // 潜影盒是否可以更新抑制
    @Unique
    private static boolean canUpdateSuppression(World world, BlockPos pos) {
        if ("false".equalsIgnoreCase(CarpetOrgAdditionSettings.CCEUpdateSuppression)) {
            return false;
        }
        if (world.getBlockEntity(pos) instanceof ShulkerBoxBlockEntity shulkerBoxBlockEntity) {
            String blockName = shulkerBoxBlockEntity.getDisplayName().getString();
            if ("true".equalsIgnoreCase(CarpetOrgAdditionSettings.CCEUpdateSuppression)) {
                return "更新抑制器".equals(blockName) || "updateSuppression".equalsIgnoreCase(blockName);
            }
            if (blockName == null) {
                return false;
            }
            // 比较字符串并忽略大小写
            return Objects.equals(CarpetOrgAdditionSettings.CCEUpdateSuppression.toLowerCase(), blockName.toLowerCase());
        }
        return false;
    }

    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        // 提示玩家不能打开用于更新抑制的潜影盒
        if (canUpdateSuppression(world, pos)) {
            MessageUtils.sendMessageToHud(player, TextUtils.translate("carpet.rule.message.CCEUpdateSuppression"));
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }
}
