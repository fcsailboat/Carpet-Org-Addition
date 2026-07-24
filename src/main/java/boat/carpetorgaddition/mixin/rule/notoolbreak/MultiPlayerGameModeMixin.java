package boat.carpetorgaddition.mixin.rule.notoolbreak;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.client.util.ClientUtils;
import boat.carpetorgaddition.rule.RuleUtils;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
    @WrapMethod(method = "useItem")
    private InteractionResult useItem(Player player, InteractionHand hand, Operation<InteractionResult> original) {
        boolean noBreak = RuleUtils.isToolNoBreak(player.getItemInHand(hand), player);
        return ScopedValue.where(CarpetOrgAdditionSettings.USE_TOOL_ITEM_NO_BREAK, noBreak).call(() -> original.call(player, hand));
    }

    @WrapMethod(method = "useItemOn")
    private InteractionResult useItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult blockHit, Operation<InteractionResult> original) {
        boolean noBreak = RuleUtils.isToolNoBreak(player.getItemInHand(hand), player);
        return ScopedValue.where(CarpetOrgAdditionSettings.USE_TOOL_ITEM_NO_BREAK, noBreak).call(() -> original.call(player, hand, blockHit));
    }

    @WrapMethod(method = "startDestroyBlock")
    private boolean startDestroyBlock(BlockPos pos, Direction direction, Operation<Boolean> original) {
        LocalPlayer player = ClientUtils.getPlayer();
        boolean noBreak = RuleUtils.isToolNoBreak(player.getMainHandItem(), player);
        return ScopedValue.where(CarpetOrgAdditionSettings.USE_TOOL_ITEM_NO_BREAK, noBreak).call(() -> original.call(pos, direction));
    }

    @WrapMethod(method = "continueDestroyBlock")
    private boolean continueDestroyBlock(BlockPos pos, Direction direction, Operation<Boolean> original) {
        LocalPlayer player = ClientUtils.getPlayer();
        boolean noBreak = RuleUtils.isToolNoBreak(player.getMainHandItem(), player);
        return ScopedValue.where(CarpetOrgAdditionSettings.USE_TOOL_ITEM_NO_BREAK, noBreak).call(() -> original.call(pos, direction));
    }
}
