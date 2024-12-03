package org.carpetorgaddition.mixin.rule.shulkerboxstackable;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.vehicle.HopperMinecartEntity;
import org.carpetorgaddition.rule.RuleUtils;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(HopperMinecartEntity.class)
public class HopperMinecartEntityMixin {
    @WrapMethod(method = "tick")
    private void tick(Operation<Void> original) {
        RuleUtils.shulkerBoxStackableWrap(original::call);
    }
}
