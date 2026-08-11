package boat.carpetorgaddition.mixin.rule.notoolbreak;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.rule.RuleUtils;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {
    @Shadow
    @Final
    protected ServerPlayer player;

    @WrapMethod(method = "useItem")
    private InteractionResult useItem(ServerPlayer player, Level level, ItemStack itemStack, InteractionHand hand, Operation<InteractionResult> original) {
        boolean noBreak = RuleUtils.isToolNoBreak(itemStack, player);
        return ScopedValue.where(CarpetOrgAdditionSettings.USE_TOOL_ITEM_NO_BREAK, noBreak)
                .call(() -> original.call(player, level, CarpetOrgAdditionSettings.USE_TOOL_ITEM_NO_BREAK.orElse(false) ? ItemStack.EMPTY : itemStack, hand));
    }

    @WrapMethod(method = "useItemOn")
    private InteractionResult useItemOn(ServerPlayer player, Level level, ItemStack itemStack, InteractionHand hand, BlockHitResult hitResult, Operation<InteractionResult> original) {
        boolean noBreak = RuleUtils.isToolNoBreak(itemStack, player);
        return ScopedValue.where(CarpetOrgAdditionSettings.USE_TOOL_ITEM_NO_BREAK, noBreak)
                .call(() -> original.call(player, level, CarpetOrgAdditionSettings.USE_TOOL_ITEM_NO_BREAK.orElse(false) ? ItemStack.EMPTY : itemStack, hand, hitResult));
    }

    @WrapMethod(method = "tick")
    private void tick(Operation<Void> original) {
        boolean noBreak = RuleUtils.isToolNoBreak(this.player.getMainHandItem(), this.player);
        ScopedValue.where(CarpetOrgAdditionSettings.USE_TOOL_ITEM_NO_BREAK, noBreak).run(original::call);
    }

    @WrapMethod(method = "handleBlockBreakAction")
    private void handleBlockBreakAction(BlockPos pos, ServerboundPlayerActionPacket.Action action, Direction direction, int maxY, int sequence, Operation<Void> original) {
        boolean noBreak = RuleUtils.isToolNoBreak(this.player.getMainHandItem(), this.player);
        ScopedValue.where(CarpetOrgAdditionSettings.USE_TOOL_ITEM_NO_BREAK, noBreak)
                .run(() -> original.call(pos, action, direction, maxY, sequence));
    }
}
