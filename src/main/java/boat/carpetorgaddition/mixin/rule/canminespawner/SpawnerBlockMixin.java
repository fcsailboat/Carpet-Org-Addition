package boat.carpetorgaddition.mixin.rule.canminespawner;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.util.EnchantmentUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 可采集刷怪笼
@Mixin(SpawnerBlock.class)
public abstract class SpawnerBlockMixin extends BaseEntityBlock {
    @SuppressWarnings("unused")
    private SpawnerBlockMixin(Properties settings) {
        super(settings);
    }

    @Inject(method = "spawnAfterBreak", at = @At("HEAD"), cancellable = true)
    // 使用精准采集工具挖掘时不会掉落经验
    private void onStacksDropped(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropExperience, CallbackInfo ci) {
        if (CarpetOrgAdditionSettings.CAN_MINE_SPAWNER.value() && EnchantmentUtils.hasSilkTouch(tool)) {
            super.spawnAfterBreak(state, level, pos, tool, dropExperience);
            ci.cancel();
        }
    }
}
