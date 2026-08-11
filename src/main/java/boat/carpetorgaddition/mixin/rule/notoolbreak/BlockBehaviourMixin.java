package boat.carpetorgaddition.mixin.rule.notoolbreak;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.rule.RuleUtils;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockBehaviour.class)
public class BlockBehaviourMixin {
    @WrapMethod(method = "getDestroyProgress")
    private float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos, Operation<Float> original) {
        boolean noBreak = RuleUtils.isToolNoBreak(player.getMainHandItem(), player);
        return ScopedValue.where(CarpetOrgAdditionSettings.USE_TOOL_ITEM_NO_BREAK, noBreak).call(() -> original.call(state, player, level, pos));
    }
}
