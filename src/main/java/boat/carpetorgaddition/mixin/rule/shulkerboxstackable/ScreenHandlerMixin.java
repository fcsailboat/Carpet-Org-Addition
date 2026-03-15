package boat.carpetorgaddition.mixin.rule.shulkerboxstackable;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractContainerMenu.class)
public abstract class ScreenHandlerMixin {
    @Shadow
    public abstract Slot getSlot(int index);

    // 比较器输出
    @WrapMethod(method = "getRedstoneSignalFromContainer(Lnet/minecraft/world/Container;)I")
    private static int calculateComparatorOutput(Container inventory, Operation<Integer> original) {
        // 如果与其它try...finally嵌套，可能导致本方法执行完毕，但是外层的try...finally代码块没有执行完时
        // CarpetOrgAdditionSettings.shulkerBoxStackCountChanged的值为true
        // 因此，在finally块中不直接设置true，而且设置为try之前记录的值
        boolean changed = CarpetOrgAdditionSettings.SHULKER_BOX_STACK_COUNT_CHANGED.get();
        try {
            CarpetOrgAdditionSettings.SHULKER_BOX_STACK_COUNT_CHANGED.set(false);
            return original.call(inventory);
        } finally {
            CarpetOrgAdditionSettings.SHULKER_BOX_STACK_COUNT_CHANGED.set(changed);
        }
    }
}
