package org.carpetorgaddition.mixin.rule.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.util.EnchantmentUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 击退棒
@Mixin(Enchantment.class)
public class EnchantmentMixin {
    @Unique
    private final Enchantment thisEnchantment = (Enchantment) (Object) this;

    @Inject(method = "isAcceptableItem", at = @At("HEAD"), cancellable = true)
    public void isAcceptableItem(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (CarpetOrgAdditionSettings.knockbackStick && stack.isOf(Items.STICK)) {
            PlayerEntity player = CarpetOrgAdditionSettings.enchanter.get();
            if (player == null) {
                return;
            }
            if (EnchantmentUtils.isSpecified(player.getWorld(), Enchantments.KNOCKBACK, thisEnchantment)) {
                cir.setReturnValue(true);
            }
        }
    }
}
