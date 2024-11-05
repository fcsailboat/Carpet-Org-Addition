package org.carpetorgaddition.mixin.rule.shulkerboxstackable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.block.BlockState;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.Hopper;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.BooleanSupplier;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin extends BlockEntity {
    @Shadow
    private Direction facing;

    public HopperBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Shadow
    private static boolean isInventoryFull(Inventory inventory, Direction direction) {
        return false;
    }

    @Shadow
    @Nullable
    private static Inventory getOutputInventory(World world, BlockPos pos, HopperBlockEntity blockEntity) {
        return null;
    }

    @Shadow
    public static ItemStack transfer(@Nullable Inventory from, Inventory to, ItemStack stack, @Nullable Direction side) {
        return null;
    }

    @Shadow
    @Nullable
    private static Inventory getInputInventory(World world, Hopper hopper, BlockPos pos, BlockState state) {
        return null;
    }

    @Shadow
    private static int[] getAvailableSlots(Inventory inventory, Direction side) {
        return new int[]{};
    }

    @Shadow
    private static boolean extract(Hopper hopper, Inventory inventory, int slot, Direction side) {
        return false;
    }

    @Shadow
    public static List<ItemEntity> getInputItemEntities(World world, Hopper hopper) {
        return List.of();
    }

    @Shadow
    public static boolean extract(Inventory inventory, ItemEntity itemEntity) {
        return false;
    }

    @Shadow
    private static boolean insert(World world, BlockPos pos, HopperBlockEntity blockEntity) {
        return false;
    }

    @Shadow
    protected abstract boolean needsCooldown();

    @Shadow
    protected abstract boolean isFull();

    @Shadow
    protected abstract void setTransferCooldown(int transferCooldown);

    // 漏斗
    @WrapOperation(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/HopperBlockEntity;insertAndExtract(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/block/entity/HopperBlockEntity;Ljava/util/function/BooleanSupplier;)Z"))
    private static boolean insert(World world, BlockPos pos, BlockState state, HopperBlockEntity blockEntity, BooleanSupplier booleanSupplier, Operation<Boolean> original) {
        if (CarpetOrgAdditionSettings.LITHIUM) {
            return original.call(world, pos, state, blockEntity, booleanSupplier);
        }
        try {
            CarpetOrgAdditionSettings.shulkerBoxStackCountChanged.set(false);
            return original.call(world, pos, state, blockEntity, booleanSupplier);
        } finally {
            CarpetOrgAdditionSettings.shulkerBoxStackCountChanged.set(true);
        }
    }

    /**
     * 保持与锂的兼容
     */
    @Unique
    private static void compatible(Runnable runnable) {
        if (CarpetOrgAdditionSettings.shulkerBoxStackable && CarpetOrgAdditionSettings.LITHIUM) {
            try {
                CarpetOrgAdditionSettings.shulkerBoxStackCountChanged.set(false);
                runnable.run();
            } finally {
                CarpetOrgAdditionSettings.shulkerBoxStackCountChanged.set(true);
            }
        }
    }

    @Inject(method = "insertAndExtract", at = @At("HEAD"), cancellable = true)
    private static void insertAndExtract(World world, BlockPos pos, BlockState state, HopperBlockEntity blockEntity, BooleanSupplier booleanSupplier, CallbackInfoReturnable<Boolean> cir) {
        compatible(() -> cir.setReturnValue(tryInsertAndExtract(world, pos, state, blockEntity, booleanSupplier)));
    }

    @Inject(method = "insert", at = @At("HEAD"), cancellable = true)
    private static void insert(World world, BlockPos pos, HopperBlockEntity blockEntity, CallbackInfoReturnable<Boolean> cir) {
        compatible(() -> cir.setReturnValue(tryInsert(world, pos, blockEntity)));
    }

    @Inject(method = "extract(Lnet/minecraft/world/World;Lnet/minecraft/block/entity/Hopper;)Z", at = @At("HEAD"), cancellable = true)
    private static void extract(World world, Hopper hopper, CallbackInfoReturnable<Boolean> cir) {
        compatible(() -> cir.setReturnValue(tryExtract(world, hopper)));

    }

    @Unique
    private static boolean tryInsertAndExtract(World world, BlockPos pos, BlockState state, HopperBlockEntity blockEntity, BooleanSupplier booleanSupplier) {
        if (world.isClient) {
            return false;
        }
        HopperBlockEntityMixin mixin = (HopperBlockEntityMixin) (Object) blockEntity;
        //noinspection DataFlowIssue
        if (!mixin.needsCooldown() && state.get(HopperBlock.ENABLED)) {
            boolean bl = false;
            if (!blockEntity.isEmpty()) {
                bl = insert(world, pos, blockEntity);
            }
            if (!mixin.isFull()) {
                bl |= booleanSupplier.getAsBoolean();
            }
            if (bl) {
                mixin.setTransferCooldown(8);
                markDirty(world, pos, state);
                return true;
            }
        }
        return false;
    }

    @Unique
    private static boolean tryInsert(World world, BlockPos pos, HopperBlockEntity blockEntity) {
        Inventory inventory = getOutputInventory(world, pos, blockEntity);
        if (inventory == null) {
            return false;
        }
        Direction direction = ((HopperBlockEntityMixin) (Object) blockEntity).facing.getOpposite();
        if (isInventoryFull(inventory, direction)) {
            return false;
        }
        for (int i = 0; i < blockEntity.size(); ++i) {
            ItemStack itemStack = blockEntity.getStack(i);
            if (!itemStack.isEmpty()) {
                int j = itemStack.getCount();
                ItemStack itemStack2 = transfer(blockEntity, inventory, blockEntity.removeStack(i, 1), direction);
                //noinspection DataFlowIssue
                if (itemStack2.isEmpty()) {
                    inventory.markDirty();
                    return true;
                }
                itemStack.setCount(j);
                if (j == 1) {
                    blockEntity.setStack(i, itemStack);
                }
            }
        }
        return false;
    }

    @Unique
    private static boolean tryExtract(World world, Hopper hopper) {
        BlockPos blockPos = BlockPos.ofFloored(hopper.getHopperX(), hopper.getHopperY() + 1.0, hopper.getHopperZ());
        BlockState blockState = world.getBlockState(blockPos);
        Inventory inventory = getInputInventory(world, hopper, blockPos, blockState);
        if (inventory != null) {
            Direction direction = Direction.DOWN;
            int[] var11 = getAvailableSlots(inventory, direction);
            for (int i : var11) {
                if (extract(hopper, inventory, i, direction)) {
                    return true;
                }
            }
        } else {
            boolean bl = hopper.canBlockFromAbove() && blockState.isFullCube(world, blockPos) && !blockState.isIn(BlockTags.DOES_NOT_BLOCK_HOPPERS);
            if (!bl) {
                for (ItemEntity itemEntity : getInputItemEntities(world, hopper)) {
                    if (extract(hopper, itemEntity)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
