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
        return ScopedValue.where(CarpetOrgAdditionSettings.SHULKER_BOX_STACK_COUNT_CHANGED, false)
                .call(() -> original.call(inventory));
    }
}
