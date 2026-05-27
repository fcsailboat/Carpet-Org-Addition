package boat.carpetorgaddition.mixin.rule.respawnblocksexplode;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 禁止重生方块爆炸
@Mixin(BedBlock.class)
public class BedBlockMixin {
    // 禁止床爆炸
    @Inject(method = "useWithoutItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/attribute/BedRule;errorMessage()Ljava/util/Optional;"), cancellable = true)
    private void onUse(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (CarpetOrgAdditionSettings.DISABLE_RESPAWN_BLOCKS_EXPLODE.value()) {
            MessageUtils.sendMessageToHud(player, LocalizationKeys.Rule.Message.DISABLE_RESPAWN_BLOCKS_EXPLODE.translate());
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}
