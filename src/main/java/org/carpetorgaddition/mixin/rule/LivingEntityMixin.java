package org.carpetorgaddition.mixin.rule;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.collection.DefaultedList;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.rule.value.BetterTotemOfUndying;
import org.carpetorgaddition.util.InventoryUtils;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    //创造玩家免疫/kill
    @Inject(method = "kill", at = @At("HEAD"), cancellable = true)
    private void kill(CallbackInfo ci) {
        if (CarpetOrgAdditionSettings.creativeImmuneKill) {
            LivingEntity livingEntity = (LivingEntity) (Object) this;
            if (livingEntity instanceof PlayerEntity player) {
                if (player.isCreative()) {
                    ci.cancel();
                }
            }
        }
    }

    //禁用伤害免疫
    @WrapOperation(method = "damage", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;timeUntilRegen:I", opcode = Opcodes.GETFIELD))
    private int setTimeUntilRegen(LivingEntity instance, Operation<Integer> original) {
        if (CarpetOrgAdditionSettings.disableDamageImmunity) {
            return 0;
        }
        return original.call(instance);
    }

    // 增强不死图腾
    @Inject(method = "tryUseTotem", at = @At("HEAD"), cancellable = true)
    private void tryUseTotem(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        // 在一开始就对规则是否开启进行判断，这样当其他Mod也修改了此段代码时，就可以通过关闭改规则来保障其他Mod的正常运行
        if (CarpetOrgAdditionSettings.betterTotemOfUndying == BetterTotemOfUndying.FALSE) {
            return;
        }
        LivingEntity thisLivingEntity = (LivingEntity) (Object) this;
        if (source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            cir.setReturnValue(false);
            return;
        }
        ItemStack itemStack = null;
        // 原版：从手上获取物品
        for (Hand hand : Hand.values()) {
            ItemStack itemStack2 = thisLivingEntity.getStackInHand(hand);
            if (!itemStack2.isOf(Items.TOTEM_OF_UNDYING)) continue;
            itemStack = itemStack2.copy();
            itemStack2.decrement(1);
            break;
        }
        // 从玩家物品栏寻找不死图腾
        if (itemStack == null && thisLivingEntity instanceof PlayerEntity playerEntity) {
            itemStack = pickTotem(playerEntity);
        }
        if (itemStack != null) {
            if (thisLivingEntity instanceof ServerPlayerEntity serverPlayerEntity) {
                serverPlayerEntity.incrementStat(Stats.USED.getOrCreateStat(Items.TOTEM_OF_UNDYING));
                Criteria.USED_TOTEM.trigger(serverPlayerEntity, itemStack);
            }
            thisLivingEntity.setHealth(1.0f);
            thisLivingEntity.clearStatusEffects();
            thisLivingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 900, 1));
            thisLivingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 100, 1));
            thisLivingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 800, 0));
            thisLivingEntity.getWorld().sendEntityStatus(thisLivingEntity, EntityStatuses.USE_TOTEM_OF_UNDYING);
        }
        cir.setReturnValue(itemStack != null);
    }

    @Unique
    @Nullable
    // 从物品栏获取不死图腾
    private static ItemStack pickTotem(PlayerEntity playerEntity) {
        DefaultedList<ItemStack> mainInventory = playerEntity.getInventory().main;
        // 从物品栏获取物品，在Inject方法的一开始就判断了规则值是否为false，所以在这里不需要再次判断
        // 无论规则值是true还是shulker_box，都需要从物品栏获取物品
        for (ItemStack totemOfUndying : mainInventory) {
            if (totemOfUndying.isOf(Items.TOTEM_OF_UNDYING)) {
                ItemStack itemStack = totemOfUndying.copy();
                totemOfUndying.decrement(1);
                return itemStack;
            }
        }
        // 如果这里规则值为true，或者说规则值不是shulker_box，那就没有必要继续向下执行
        if (CarpetOrgAdditionSettings.betterTotemOfUndying == BetterTotemOfUndying.TRUE) {
            return null;
        }
        // 如果执行到这里，那么规则值一定是shulker_box，因为如果是true会在上面的if语句中直接返回，如果为false，这个方法都不会被执行
        for (ItemStack shulkerBox : mainInventory) {
            if (InventoryUtils.isShulkerBoxItem(shulkerBox)) {
                // 从潜影盒中拿取不死图腾
                ItemStack itemInTheBox = InventoryUtils.shulkerBoxConsumer(
                        shulkerBox,
                        stack -> stack.isOf(Items.TOTEM_OF_UNDYING),
                        stack -> stack.decrement(1)
                );
                // 潜影盒中可能没有不死图腾
                if (itemInTheBox.isEmpty()) {
                    continue;
                }
                return itemInTheBox.copy();
            }
        }
        return null;
    }
}