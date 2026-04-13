package boat.carpetorgaddition.mixin.rule.forceopencontainer;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(ShulkerBoxBlock.class)
public class ShulkerBoxBlockMixin {
    // 强制开启潜影盒
    @Inject(method = "canOpen", at = @At(value = "HEAD"), cancellable = true)
    private static void canOpen(BlockState state, Level level, BlockPos pos, ShulkerBoxBlockEntity blockEntity, CallbackInfoReturnable<Boolean> cir) {
        if (CarpetOrgAdditionSettings.FORCE_OPEN_CONTAINER.value().canOpenShulkerBox()) {
            cir.setReturnValue(true);
        }
    }
}
