package org.carpetorgaddition.mixin.rule.blockdropsdirectlyenterinventory;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(Block.class)
public abstract class BlockMixin {
    // 方块掉落物直接进入物品栏
    @WrapMethod(method = "getDroppedStacks(Lnet/minecraft/block/BlockState;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/BlockEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)Ljava/util/List;")
    private static List<ItemStack> getDroppedStacks(BlockState state, ServerWorld world, BlockPos pos, @Nullable BlockEntity blockEntity, @Nullable Entity entity, ItemStack stack, Operation<List<ItemStack>> original) {
        // 获取本来要掉落的物品
        List<ItemStack> list = original.call(state, world, pos, blockEntity, entity, stack);
        return collect(list);
    }

    @WrapMethod(method = "getDroppedStacks(Lnet/minecraft/block/BlockState;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/BlockEntity;)Ljava/util/List;")
    private static List<ItemStack> getDroppedStacks(BlockState state, ServerWorld world, BlockPos pos, @Nullable BlockEntity blockEntity, Operation<List<ItemStack>> original) {
        List<ItemStack> list = original.call(state, world, pos, blockEntity);
        return collect(list);
    }

    /**
     * @param list 原本要掉落的物品
     * @return 将物品插入物品栏后剩余的物品
     */
    @Unique
    private static List<ItemStack> collect(List<ItemStack> list) {
        if (CarpetOrgAdditionSettings.blockDropsDirectlyEnterInventory) {
            ServerPlayerEntity player = CarpetOrgAdditionSettings.blockBreaking.get();
            if (player == null) {
                return list;
            }
            // 将物品直接插入玩家物品栏
            for (ItemStack itemStack : list) {
                player.getInventory().insertStack(itemStack);
            }
            // 如果物品完全插入玩家物品栏，返回空集合，否则将剩余物品返回，然后掉落
            return list.isEmpty() ? List.of() : list.stream().filter(itemStack -> !itemStack.isEmpty()).toList();
        }
        return list;
    }
}