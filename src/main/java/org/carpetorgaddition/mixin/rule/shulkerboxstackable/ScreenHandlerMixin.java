package org.carpetorgaddition.mixin.rule.shulkerboxstackable;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.carpetorgaddition.CarpetOrgAddition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ScreenHandler.class)
public abstract class ScreenHandlerMixin {
    @Shadow
    public abstract Slot getSlot(int index);

    // 比较器输出
    @WrapMethod(method = "calculateComparatorOutput(Lnet/minecraft/inventory/Inventory;)I")
    private static int calculateComparatorOutput(Inventory inventory, Operation<Integer> original) {
        try {
            CarpetOrgAddition.shulkerBoxStackCountChanged.set(false);
            return original.call(inventory);
        } finally {
            CarpetOrgAddition.shulkerBoxStackCountChanged.set(true);
        }
    }
}
