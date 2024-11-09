package org.carpetorgaddition.mixin.rule.shulkerboxstackable;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ScreenHandler.class)
public abstract class ScreenHandlerMixin {
    @Shadow
    public abstract Slot getSlot(int index);

    // 比较器输出
    @WrapMethod(method = "calculateComparatorOutput(Lnet/minecraft/inventory/Inventory;)I")
    private static int calculateComparatorOutput(Inventory inventory, Operation<Integer> original) {
        // 如果与其它try...finally嵌套，可能导致本方法执行完毕，但是外层的try...finally代码块没有执行完时
        // CarpetOrgAdditionSettings.shulkerBoxStackCountChanged的值为true
        // 因此，在finally块中不直接设置true，而且设置为try之前记录的值
        boolean changed = CarpetOrgAdditionSettings.shulkerBoxStackCountChanged.get();
        try {
            CarpetOrgAdditionSettings.shulkerBoxStackCountChanged.set(false);
            return original.call(inventory);
        } finally {
            CarpetOrgAdditionSettings.shulkerBoxStackCountChanged.set(changed);
        }
    }
}
