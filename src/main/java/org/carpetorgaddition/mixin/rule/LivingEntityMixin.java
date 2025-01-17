package org.carpetorgaddition.mixin.rule;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DeathProtectionComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.rule.value.BetterTotemOfUndying;
import org.carpetorgaddition.util.InventoryUtils;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    protected LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Shadow
    public abstract ItemStack getStackInHand(Hand hand);

    @Shadow
    public abstract void setHealth(float health);

    @Shadow
    @Nullable
    protected abstract Map<EquipmentSlot, ItemStack> getEquipmentChanges();

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
    @Inject(method = "tryUseDeathProtector", at = @At("HEAD"), cancellable = true)
    private void tryUseTotem(DamageSource source, CallbackInfoReturnable<Boolean> cir) {        // 在一开始就对规则是否开启进行判断，这样当其他Mod也修改了此段代码时，就可以通过关闭改规则来保障其他Mod的正常运行
        if (CarpetOrgAdditionSettings.betterTotemOfUndying == BetterTotemOfUndying.FALSE) {
            return;
        }
        LivingEntity thisLivingEntity = (LivingEntity) (Object) this;
        // 无法触发不死图腾的伤害
        if (source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            cir.setReturnValue(false);
            return;
        }
        ItemStack itemStack = null;
        DeathProtectionComponent deathProtectionComponent = null;
        // 原版：从手上获取物品
        Hand[] values = Hand.values();
        for (Hand hand : values) {
            ItemStack stack = this.getStackInHand(hand);
            deathProtectionComponent = stack.get(DataComponentTypes.DEATH_PROTECTION);
            if (deathProtectionComponent != null) {
                itemStack = stack.copy();
                stack.decrement(1);
                break;
            }
        }
        // 从玩家物品栏寻找不死图腾
        if (itemStack == null && thisLivingEntity instanceof PlayerEntity playerEntity) {
            Pair<ItemStack, DeathProtectionComponent> pair = pickTotem(playerEntity);
            if (pair != null) {
                itemStack = pair.getLeft();
                deathProtectionComponent = pair.getRight();
            }
        }
        if (itemStack != null) {
            if (thisLivingEntity instanceof ServerPlayerEntity serverPlayerEntity) {
                serverPlayerEntity.incrementStat(Stats.USED.getOrCreateStat(itemStack.getItem()));
                Criteria.USED_TOTEM.trigger(serverPlayerEntity, itemStack);
                this.emitGameEvent(GameEvent.ITEM_INTERACT_FINISH);
            }
            this.setHealth(1.0F);
            deathProtectionComponent.applyDeathEffects(itemStack, thisLivingEntity);
            this.getWorld().sendEntityStatus(this, (byte) 35);
        }
        cir.setReturnValue(deathProtectionComponent != null);
    }

    @Unique
    @Nullable
    // 从物品栏获取不死图腾
    private static Pair<ItemStack, DeathProtectionComponent> pickTotem(PlayerEntity playerEntity) {
        DefaultedList<ItemStack> mainInventory = playerEntity.getInventory().main;
        // 从物品栏获取物品，在Inject方法的一开始就判断了规则值是否为false，所以在这里不需要再次判断
        // 无论规则值是true还是shulker_box，都需要从物品栏获取物品
        for (ItemStack totemOfUndying : mainInventory) {
            DeathProtectionComponent component = totemOfUndying.get(DataComponentTypes.DEATH_PROTECTION);
            if (component == null) {
                continue;
            }
            ItemStack itemStack = totemOfUndying.copy();
            totemOfUndying.decrement(1);
            return new Pair<>(itemStack, component);
        }
        // 如果这里规则值为true，或者说规则值不是shulker_box，那就没有必要继续向下执行
        if (CarpetOrgAdditionSettings.betterTotemOfUndying == BetterTotemOfUndying.TRUE) {
            return null;
        }
        // 如果执行到这里，那么规则值一定是shulker_box，因为如果是true会在上面的if语句中直接返回，如果为false，这个方法都不会被执行
        for (ItemStack shulkerBox : mainInventory) {
            if (InventoryUtils.isShulkerBoxItem(shulkerBox)) {
                // 从潜影盒中拿取不死图腾
                ItemStack itemInTheBox = InventoryUtils.shulkerBoxConsumer(shulkerBox,
                        stack -> stack.get(DataComponentTypes.DEATH_PROTECTION) != null,
                        stack -> stack.decrement(1));
                // 潜影盒中可能没有不死图腾
                if (itemInTheBox.isEmpty()) {
                    continue;
                }
                return new Pair<>(itemInTheBox, itemInTheBox.get(DataComponentTypes.DEATH_PROTECTION));
            }
        }
        return null;
    }

    @Unique
    protected void onPlayerBreakBlock() {
        this.getEquipmentChanges();
    }
}