package boat.carpetorgaddition.mixin.rule.shulkerboxstackable;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.util.InventoryUtils;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.BooleanSupplier;

@Mixin(value = HopperBlockEntity.class, priority = 949)
public abstract class HopperBlockEntityMixin extends BlockEntity {
    public HopperBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @WrapMethod(method = "pushItemsTick")
    private static void insert(Level level, BlockPos pos, BlockState state, HopperBlockEntity entity, Operation<Void> original) {
        ScopedValue.where(CarpetOrgAdditionSettings.SHULKER_BOX_STACK_COUNT_CHANGED, false).call(() -> original.call(level, pos, state, entity));
    }

    @WrapMethod(method = "tryMoveItems")
    private static boolean transfer(Level level, BlockPos pos, BlockState state, HopperBlockEntity entity, BooleanSupplier action, Operation<Boolean> original) {
        return ScopedValue.where(CarpetOrgAdditionSettings.SHULKER_BOX_STACK_COUNT_CHANGED, false).call(() -> original.call(level, pos, state, entity, action));
    }

    @WrapMethod(method = "suckInItems")
    private static boolean suck(Level level, Hopper hopper, Operation<Boolean> original) {
        return ScopedValue.where(CarpetOrgAdditionSettings.SHULKER_BOX_STACK_COUNT_CHANGED, false).call(() -> original.call(level, hopper));
    }

    @WrapMethod(method = "ejectItems")
    private static boolean ejectItems(Level level, BlockPos blockPos, HopperBlockEntity self, Operation<Boolean> original) {
        return ScopedValue.where(CarpetOrgAdditionSettings.SHULKER_BOX_STACK_COUNT_CHANGED, false).call(() -> original.call(level, blockPos, self));
    }

    @WrapMethod(method = "addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/entity/item/ItemEntity;)Z")
    private static boolean addItem(Container container, ItemEntity entity, Operation<Boolean> original) {
        return ScopedValue.where(CarpetOrgAdditionSettings.SHULKER_BOX_STACK_COUNT_CHANGED, false).call(() -> original.call(container, entity));
    }

    @WrapMethod(method = "addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;")
    private static ItemStack addItem(Container from, Container container, ItemStack itemStack, Direction direction, Operation<ItemStack> original) {
        return ScopedValue.where(CarpetOrgAdditionSettings.SHULKER_BOX_STACK_COUNT_CHANGED, false).call(() -> original.call(from, container, itemStack, direction));
    }

    /**
     * 让漏斗一次从一堆掉落物中只吸取一个潜影盒
     */
    @WrapOperation(method = "addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/entity/item/ItemEntity;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack extract(Container from, Container to, ItemStack stack, Direction side, Operation<ItemStack> original, @Local(name = "changed") LocalBooleanRef bl) {
        if (CarpetOrgAdditionSettings.SHULKER_BOX_STACK_COUNT_CHANGED.orElse(true)) {
            return original.call(from, to, stack, side);
        }
        if (CarpetOrgAdditionSettings.SHULKER_BOX_STACKABLE.value() && InventoryUtils.isShulkerBoxItem(stack)) {
            ItemStack split = stack.split(stack.getMaxStackSize());
            int count = split.getCount();
            ItemStack result = original.call(from, to, split.copy(), side);
            stack.grow(result.getCount());
            if (count != result.getCount()) {
                bl.set(true);
            }
            return stack;
        }
        return original.call(from, to, stack, side);
    }
}
