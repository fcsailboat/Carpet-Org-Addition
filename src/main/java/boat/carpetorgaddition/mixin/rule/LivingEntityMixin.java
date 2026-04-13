package boat.carpetorgaddition.mixin.rule;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.rule.value.BetterTotemOfUndying;
import boat.carpetorgaddition.util.InventoryUtils;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DeathProtection;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Map;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Unique
    private final LivingEntity self = (LivingEntity) (Object) this;

    /**
     * 在 {@link PlayerEntityMixin#getBlockBreakingSpeed(BlockState, CallbackInfoReturnable)}中被使用
     */
    @Shadow
    @SuppressWarnings("JavadocReference")
    private Map<EquipmentSlot, ItemStack> collectEquipmentChanges() {
        return Map.of();
    }

    // 禁用伤害免疫
    @WrapOperation(method = "hurtServer", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;invulnerableTime:I", opcode = Opcodes.GETFIELD))
    private int setTimeUntilRegen(LivingEntity instance, Operation<Integer> original) {
        if (CarpetOrgAdditionSettings.DISABLE_DAMAGE_IMMUNITY.value()) {
            return 0;
        }
        return original.call(instance);
    }

    // 不死图腾无敌时间
    @Inject(method = "checkTotemDeathProtection", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;broadcastEntityEvent(Lnet/minecraft/world/entity/Entity;B)V"))
    private void setInvincibleTime(DamageSource killingDamage, CallbackInfoReturnable<Boolean> cir) {
        if (CarpetOrgAdditionSettings.TOTEM_OF_UNDYING_INVINCIBLE_TIME.value()) {
            this.self.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 4));
        }
    }

    // 增强不死图腾
    @Definition(id = "itemStack", local = @Local(type = ItemStack.class, ordinal = 0))
    @Expression("itemStack != null")
    @ModifyExpressionValue(method = "checkTotemDeathProtection", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean tryUseTotem(boolean original, @Local(name = "protectionItem") LocalRef<ItemStack> stackRef, @Local(name = "protection") LocalRef<DeathProtection> componentRef) {
        if (original) {
            return true;
        }
        if (CarpetOrgAdditionSettings.BETTER_TOTEM_OF_UNDYING.value() == BetterTotemOfUndying.VANILLA) {
            return false;
        }
        if (this.self instanceof Player player) {
            ItemStack itemStack = pickTotem(player);
            if (itemStack.isEmpty()) {
                return false;
            }
            stackRef.set(itemStack);
            componentRef.set(itemStack.get(DataComponents.DEATH_PROTECTION));
            return true;
        }
        return false;
    }

    @Unique
    @NotNull
    // 从物品栏获取不死图腾
    private static ItemStack pickTotem(Player player) {
        NonNullList<ItemStack> mainInventory = player.getInventory().getNonEquipmentItems();
        // 无论规则值是true还是shulker_box，都需要从物品栏获取物品
        for (ItemStack itemStack : mainInventory) {
            if (itemStack.isEmpty()) {
                continue;
            }
            if (InventoryUtils.isTotemItem(itemStack)) {
                return itemStack.split(1);
            }
        }
        // 如果这里规则值为true，或者说规则值不是shulker_box，那就没有必要继续向下执行
        if (CarpetOrgAdditionSettings.BETTER_TOTEM_OF_UNDYING.value() == BetterTotemOfUndying.INVENTORY) {
            return ItemStack.EMPTY;
        }
        ArrayList<ItemStack> list = new ArrayList<>(mainInventory.size());
        for (ItemStack shulker : mainInventory) {
            if (shulker.isEmpty()) {
                continue;
            }
            if (InventoryUtils.isOperableSulkerBox(shulker)) {
                // 从潜影盒中拿取不死图腾
                ItemStack itemStack = InventoryUtils.pickItemFromShulkerBox(shulker, InventoryUtils::isTotemItem, 1);
                // 潜影盒中可能没有不死图腾
                if (itemStack.isEmpty()) {
                    continue;
                }
                return itemStack.split(1);
            } else if (InventoryUtils.isShulkerBoxItem(shulker)) {
                list.add(shulker);
            }
        }
        // 从堆叠的非空潜影盒中获取不死图腾
        for (ItemStack shulker : list) {
            ItemStack itemStack = InventoryUtils.tryPickItemFromStackedNonEmptyShulkerBox(player, shulker, InventoryUtils::isTotemItem);
            if (itemStack.isEmpty()) {
                continue;
            }
            return itemStack;
        }
        return ItemStack.EMPTY;
    }

    @Unique
    @SuppressWarnings("UnusedReturnValue")
    protected Map<EquipmentSlot, ItemStack> applyToolEffects() {
        return this.collectEquipmentChanges();
    }
}