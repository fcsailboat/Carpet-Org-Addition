package boat.carpetorgaddition.mixin.rule.forceopencontainer;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.rule.RuleUtils;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChestBlock.class)
public class ChestBlockMixin {
    @WrapOperation(method = "useWithoutItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/ChestBlock;getMenuProvider(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/MenuProvider;"))
    private static MenuProvider isChestBlockedAt(ChestBlock instance, BlockState state, Level level, BlockPos pos, Operation<MenuProvider> original) {
        return ScopedValue.where(RuleUtils.OPENING_THE_CHEST, CarpetOrgAdditionSettings.FORCE_OPEN_CONTAINER.value().canOpenChest())
                .call(() -> original.call(instance, state, level, pos));
    }

    @Inject(method = "isChestBlockedAt", at = @At("HEAD"), cancellable = true)
    private static void isChestBlockedAt(LevelAccessor level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        // 仅在打开箱子时修改返回值，避免影响比较器逻辑（无法打开的箱子比较器信号强度为0）
        if (RuleUtils.OPENING_THE_CHEST.orElse(false)) {
            cir.setReturnValue(false);
        }
    }
}
