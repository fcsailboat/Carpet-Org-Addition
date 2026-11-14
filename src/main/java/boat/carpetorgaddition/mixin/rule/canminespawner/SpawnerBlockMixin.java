package boat.carpetorgaddition.mixin.rule.canminespawner;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.util.EnchantmentUtils;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SpawnerBlock.class)
public abstract class SpawnerBlockMixin extends BaseEntityBlock {
    @SuppressWarnings("unused")
    private SpawnerBlockMixin(Properties settings) {
        super(settings);
    }

    // 使用精准采集工具挖掘时不会掉落经验
    @WrapMethod(method = "spawnAfterBreak")
    private void onStacksDropped(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropExperience, Operation<Void> original) {
        if (CarpetOrgAdditionSettings.CAN_MINE_SPAWNER.value() && EnchantmentUtils.hasSilkTouch(tool)) {
            original.call(state, level, pos, tool, false);
        } else {
            original.call(state, level, pos, tool, dropExperience);
        }
    }
}
