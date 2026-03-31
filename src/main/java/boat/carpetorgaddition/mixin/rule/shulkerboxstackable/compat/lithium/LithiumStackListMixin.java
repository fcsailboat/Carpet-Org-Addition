package boat.carpetorgaddition.mixin.rule.shulkerboxstackable.compat.lithium;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.compat.Depend;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.caffeinemc.mods.lithium.common.hopper.LithiumStackList;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Depend("lithium")
@Mixin(LithiumStackList.class)
public class LithiumStackListMixin {
    @WrapOperation(method = {
            "<init>(Lnet/minecraft/core/NonNullList;I)V",
            "changedALot",
            "set(ILnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;",
            "calculateSignalStrength",
            "lithium$notifyCount(Lnet/minecraft/world/item/ItemStack;II)V"
    }, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I"))
    private int modifyCountSize(ItemStack instance, Operation<Integer> original) {
        return ScopedValue.where(CarpetOrgAdditionSettings.SHULKER_BOX_STACK_COUNT_CHANGED, false).call(() -> original.call(instance));
    }
}
