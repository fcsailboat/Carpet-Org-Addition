package boat.carpetorgaddition.mixin.rule.disableanvilexpensive;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AnvilMenu.class)
public class AnvilScreenHandlerMixin {
    @WrapOperation(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getCount()I", ordinal = 1))
    private int markInvalid(ItemStack instance, Operation<Integer> original, @Local(name = "price") LocalIntRef ref) {
        int call = original.call(instance);
        int value = CarpetOrgAdditionSettings.SET_ANVIL_EXPERIENCE_CONSUMPTION_LIMIT.value();
        if (value == -1 || call <= 1) {
            return call;
        }
        ref.set(value + 1);
        return 1;
    }

    @ModifyExpressionValue(method = "createResult", at = @At(value = "CONSTANT", args = "intValue=40", ordinal = 2))
    private int updateResult(int original) {
        // 重命名物品的最大等级仍为39级
        int value = CarpetOrgAdditionSettings.SET_ANVIL_EXPERIENCE_CONSUMPTION_LIMIT.value();
        if (value == -1) {
            return original;
        }
        return value;
    }
}
