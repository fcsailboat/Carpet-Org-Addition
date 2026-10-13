package boat.carpetorgaddition.mixin.rule.canminespawner;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.util.EnchantmentUtils;
import boat.carpetorgaddition.util.ServerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TrialSpawnerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(Block.class)
public abstract class BlockMixin {
    @Shadow
    public abstract Item asItem();

    @Unique
    private final Block self = (Block) (Object) this;

    @Inject(method = "playerWillDestroy", at = @At("HEAD"))
    private void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player, CallbackInfoReturnable<BlockState> cir) {
        if (EnchantmentUtils.hasSilkTouch(player.getMainHandItem())
            && CarpetOrgAdditionSettings.CAN_MINE_SPAWNER.value()
            && !player.isCreative()
            && !level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            switch (blockEntity) {
                case SpawnerBlockEntity _, TrialSpawnerBlockEntity _ -> {
                    try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(blockEntity.problemPath(), CarpetOrgAddition.LOGGER)) {
                        ServerUtils.getServer(player).ifPresent(server -> this.drop(level, pos, server, reporter, blockEntity));
                    }
                }
                case null, default -> {
                }
            }
        }
    }

    @Unique
    private void drop(Level world, BlockPos pos, MinecraftServer server, ProblemReporter.ScopedCollector reporter, BlockEntity blockEntity) {
        TagValueOutput output = TagValueOutput.createWithContext(reporter, server.registryAccess());
        blockEntity.saveCustomOnly(output);
        ItemStack itemStack = new ItemStack(this.asItem());
        BlockItem.setBlockEntityData(itemStack, blockEntity.getType(), output);
        itemStack.applyComponents(blockEntity.collectComponents());
        if (this.self instanceof TrialSpawnerBlock) {
            world.getBlockState(pos).getOptionalValue(TrialSpawnerBlock.OMINOUS).ifPresent(value -> {
                Map<String, String> properties = Map.of(TrialSpawnerBlock.OMINOUS.getName(), value.toString());
                itemStack.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(properties));
            });
        }
        ItemEntity itemEntity = new ItemEntity(world, (double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5, itemStack);
        itemEntity.setDefaultPickUpDelay();
        world.addFreshEntity(itemEntity);
    }
}
