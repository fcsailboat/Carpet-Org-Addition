package org.carpetorgaddition.mixin.rule.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.util.EnchantmentUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

// 击退棒
@Mixin(Enchantment.class)
public class EnchantmentMixin {
    @Unique
    private final Enchantment thisEnchantment = (Enchantment) (Object) this;

    // 击退棒
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

    // 保护类魔咒兼容
    @Inject(method = "canBeCombined", at = @At("HEAD"), cancellable = true)
    private static void protectionEnchantmentCompatible(RegistryEntry<Enchantment> first, RegistryEntry<Enchantment> second, CallbackInfoReturnable<Boolean> cir) {
        if (CarpetOrgAdditionSettings.protectionEnchantmentCompatible && !first.equals(second)) {
            Optional<RegistryKey<Enchantment>> firstKey = first.getKey();
            Optional<RegistryKey<Enchantment>> secondKey = second.getKey();
            if (firstKey.isEmpty() || secondKey.isEmpty()) {
                return;
            }
            if (EnchantmentUtils.isProtectionEnchantment(firstKey.get()) || EnchantmentUtils.isProtectionEnchantment(secondKey.get())) {
                cir.setReturnValue(true);
            }
        }
    }

    // 伤害类魔咒兼容
    @Inject(method = "canBeCombined", at = @At("HEAD"), cancellable = true)
    private static void damageEnchantmentCompatible(RegistryEntry<Enchantment> first, RegistryEntry<Enchantment> second, CallbackInfoReturnable<Boolean> cir) {
        if (CarpetOrgAdditionSettings.damageEnchantmentCompatible && !first.equals(second)) {
            Optional<RegistryKey<Enchantment>> firstKey = first.getKey();
            Optional<RegistryKey<Enchantment>> secondKey = second.getKey();
            if (firstKey.isEmpty() || secondKey.isEmpty()) {
                return;
            }
            if (EnchantmentUtils.isDamageEnchantment(firstKey.get()) || EnchantmentUtils.isDamageEnchantment(secondKey.get())) {
                cir.setReturnValue(true);
            }
        }
    }
}
