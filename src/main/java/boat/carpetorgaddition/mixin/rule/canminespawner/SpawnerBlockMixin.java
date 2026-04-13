package boat.carpetorgaddition.mixin.rule.canminespawner;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.util.EnchantmentUtils;
import boat.carpetorgaddition.util.ServerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jspecify.annotations.NonNull;
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
        if (CarpetOrgAdditionSettings.CAN_MINE_SPAWNER.value() && EnchantmentUtils.hasEnchantment(level, Enchantments.SILK_TOUCH, tool)) {
            super.spawnAfterBreak(state, level, pos, tool, dropExperience);
            ci.cancel();
        }
    }

    @Override
    // 使用精准采集挖掘时掉落带NBT的物品
    public @NonNull BlockState playerWillDestroy(@NonNull Level world, @NonNull BlockPos pos, @NonNull BlockState state, Player player) {
        boolean hasSilkTouch = EnchantmentUtils.hasEnchantment(world, Enchantments.SILK_TOUCH, player.getMainHandItem());
        if (CarpetOrgAdditionSettings.CAN_MINE_SPAWNER.value() && !player.isCreative() && hasSilkTouch) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (!world.isClientSide() && blockEntity instanceof SpawnerBlockEntity spawner) {
                ItemStack itemStack = new ItemStack(Items.SPAWNER);
                MinecraftServer server = ServerUtils.getServer(player);
                if (server != null) {
                    TagValueOutput view = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, server.registryAccess());
                    BaseSpawner logic = spawner.getSpawner();
                    logic.save(view);
                    BlockItem.setBlockEntityData(itemStack, blockEntity.getType(), view);
                }
                ItemEntity itemEntity = new ItemEntity(world, (double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5, itemStack);
                itemEntity.setDefaultPickUpDelay();
                world.addFreshEntity(itemEntity);
            }
        }
        return super.playerWillDestroy(world, pos, state, player);
    }
}
