package boat.carpetorgaddition.mixin.rule.shulkerboxstackable;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractContainerMenu.class)
public abstract class ScreenHandlerMixin {
    // 比较器输出
    @WrapMethod(method = "getRedstoneSignalFromContainer(Lnet/minecraft/world/Container;)I")
    private static int calculateComparatorOutput(Container container, Operation<Integer> original) {
        return ScopedValue.where(CarpetOrgAdditionSettings.SHULKER_BOX_STACK_COUNT_CHANGED, false).call(() -> original.call(container));
    }
}
