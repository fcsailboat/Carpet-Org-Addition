package boat.carpetorgaddition.mixin.rule.canminespawner;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.util.EnchantmentUtils;
import boat.carpetorgaddition.util.ServerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public class BlockMixin {
    @Unique
    private final Block self = (Block) (Object) this;

    @Inject(method = "playerWillDestroy", at = @At("HEAD"))
    private void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player, CallbackInfoReturnable<BlockState> cir) {
        if (this.self instanceof SpawnerBlock
            && EnchantmentUtils.hasSilkTouch(player.getMainHandItem())
            && CarpetOrgAdditionSettings.CAN_MINE_SPAWNER.value()
            && !player.isCreative()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!level.isClientSide() && blockEntity instanceof SpawnerBlockEntity spawner) {
                ItemStack itemStack = new ItemStack(Items.SPAWNER);
                ServerUtils.getServer(player).ifPresent(server -> {
                    TagValueOutput view = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, server.registryAccess());
                    BaseSpawner logic = spawner.getSpawner();
                    logic.save(view);
                    BlockItem.setBlockEntityData(itemStack, blockEntity.getType(), view);
                });
                ItemEntity itemEntity = new ItemEntity(level, (double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5, itemStack);
                itemEntity.setDefaultPickUpDelay();
                level.addFreshEntity(itemEntity);
            }
        }
    }
}
