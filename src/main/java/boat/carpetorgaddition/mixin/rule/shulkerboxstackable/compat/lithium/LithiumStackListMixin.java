package boat.carpetorgaddition.mixin.rule.shulkerboxstackable.compat.lithium;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.compat.Depend;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.caffeinemc.mods.lithium.common.hopper.LithiumStackList;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * MIT License
 * <p>
 * Copyright (c) 2025-2026 Liuyue_awa
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
