package org.carpetorgaddition.mixin.rule.canminespawner;

import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.SpawnerBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.util.EnchantmentUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//可采集刷怪笼
@Mixin(SpawnerBlock.class)
public abstract class SpawnerBlockMixin extends BlockWithEntity {
    protected SpawnerBlockMixin(Settings settings) {
        super(settings);
    }

    @Inject(method = "onStacksDropped", at = @At("HEAD"), cancellable = true)
    // 使用精准采集工具挖掘时不会掉落经验
    private void onStacksDropped(BlockState state, ServerWorld world, BlockPos pos, ItemStack tool, boolean dropExperience, CallbackInfo ci) {
        if (CarpetOrgAdditionSettings.canMineSpawner && EnchantmentUtils.hasEnchantment(world, Enchantments.SILK_TOUCH, tool)) {
            super.onStacksDropped(state, world, pos, tool, dropExperience);
            ci.cancel();
        }
    }

    @Override
    // 使用精准采集挖掘时掉落带NBT的物品
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        boolean hasSilkTouch = EnchantmentUtils.hasEnchantment(world, Enchantments.SILK_TOUCH, player.getMainHandStack());
        if (CarpetOrgAdditionSettings.canMineSpawner && !player.isCreative() && hasSilkTouch) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (!world.isClient && blockEntity instanceof MobSpawnerBlockEntity) {
                ItemStack itemStack = new ItemStack(Items.SPAWNER);
                NbtCompound nbtCompound = blockEntity.createComponentlessNbtWithIdentifyingData(player.getWorld().getRegistryManager());
                BlockItem.setBlockEntityData(itemStack, blockEntity.getType(), nbtCompound);
                itemStack.applyComponentsFrom(blockEntity.getComponents());
                if (tryCollect(itemStack)) {
                    ItemEntity itemEntity = new ItemEntity(world, (double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5, itemStack);
                    itemEntity.setToDefaultPickupDelay();
                    world.spawnEntity(itemEntity);
                }
            }
        }
        return super.onBreak(world, pos, state, player);
    }

    // 方块掉落物直接进入物品栏
    @Unique
    private boolean tryCollect(ItemStack itemStack) {
        if (CarpetOrgAdditionSettings.blockDropsDirectlyEnterInventory) {
            ServerPlayerEntity player = CarpetOrgAdditionSettings.blockBreaking.get();
            if (player == null) {
                return true;
            }
            player.getInventory().insertStack(itemStack);
            return !itemStack.isEmpty();
        }
        return true;
    }
}
