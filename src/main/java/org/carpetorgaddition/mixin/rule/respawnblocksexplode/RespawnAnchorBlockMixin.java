package org.carpetorgaddition.mixin.rule.respawnblocksexplode;

import net.minecraft.block.BlockState;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.TextUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RespawnAnchorBlock.class)
public class RespawnAnchorBlockMixin {
    //禁止重生锚爆炸
    @Inject(method = "onUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/RespawnAnchorBlock;isNether(Lnet/minecraft/world/World;)Z"), cancellable = true)
    private void onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        if (CarpetOrgAdditionSettings.disableRespawnBlocksExplode && !RespawnAnchorBlock.isNether(world)) {
            MessageUtils.sendMessageToHud(player, TextUtils.translate("carpet.rule.message.disableRespawnBlocksExplode"));
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }
}
