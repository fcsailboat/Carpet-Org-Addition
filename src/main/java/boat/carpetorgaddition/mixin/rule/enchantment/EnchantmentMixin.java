package boat.carpetorgaddition.mixin.rule.enchantment;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.util.EnchantmentUtils;
import boat.carpetorgaddition.util.ServerUtils;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
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
    @Inject(method = "canEnchant", at = @At("HEAD"), cancellable = true)
    public void isAcceptableItem(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (CarpetOrgAdditionSettings.KNOCKBACK_STICK.value() && stack.is(Items.STICK)) {
            Player player = CarpetOrgAdditionSettings.ENCHANTER.get();
            if (player == null) {
                return;
            }
            if (EnchantmentUtils.isSpecified(ServerUtils.getWorld(player), Enchantments.KNOCKBACK, thisEnchantment)) {
                cir.setReturnValue(true);
            }
        }
    }

    // 保护类魔咒兼容
    @Inject(method = "areCompatible", at = @At("HEAD"), cancellable = true)
    private static void protectionEnchantmentCompatible(Holder<Enchantment> first, Holder<Enchantment> second, CallbackInfoReturnable<Boolean> cir) {
        if (CarpetOrgAdditionSettings.PROTECTION_ENCHANTMENT_COMPATIBLE.value() && !first.equals(second)) {
            Optional<ResourceKey<Enchantment>> firstKey = first.unwrapKey();
            Optional<ResourceKey<Enchantment>> secondKey = second.unwrapKey();
            if (firstKey.isEmpty() || secondKey.isEmpty()) {
                return;
            }
            if (EnchantmentUtils.isProtectionEnchantment(firstKey.get()) || EnchantmentUtils.isProtectionEnchantment(secondKey.get())) {
                cir.setReturnValue(true);
            }
        }
    }

    // 伤害类魔咒兼容
    @Inject(method = "areCompatible", at = @At("HEAD"), cancellable = true)
    private static void damageEnchantmentCompatible(Holder<Enchantment> first, Holder<Enchantment> second, CallbackInfoReturnable<Boolean> cir) {
        if (CarpetOrgAdditionSettings.DAMAGE_ENCHANTMENT_COMPATIBLE.value() && !first.equals(second)) {
            Optional<ResourceKey<Enchantment>> firstKey = first.unwrapKey();
            Optional<ResourceKey<Enchantment>> secondKey = second.unwrapKey();
            if (firstKey.isEmpty() || secondKey.isEmpty()) {
                return;
            }
            if (EnchantmentUtils.isDamageEnchantment(firstKey.get()) || EnchantmentUtils.isDamageEnchantment(secondKey.get())) {
                cir.setReturnValue(true);
            }
        }
    }
}
