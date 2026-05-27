package boat.carpetorgaddition.mixin.rule.channelingignoreweather;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Consumer;

// 强化引雷
@Mixin(ThrownTrident.class)
public abstract class TridentEntityMixin extends AbstractArrow {
    @SuppressWarnings("unused")
    private TridentEntityMixin(EntityType<? extends AbstractArrow> entityType, Level world) {
        super(entityType, world);
    }

    // 击中实体
    @WrapOperation(method = "onHitEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;doPostAttackEffectsWithItemSourceOnBreak(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/item/ItemStack;Ljava/util/function/Consumer;)V"))
    private void onHitEntity(ServerLevel serverLevel, Entity victim, DamageSource damageSource, ItemStack source, Consumer<Item> attackerlessOnBreak, Operation<Void> original) {
        ScopedValue.where(CarpetOrgAdditionSettings.USE_CHANNELING_TRIDENT, true)
                .call(() -> original.call(serverLevel, victim, damageSource, source, attackerlessOnBreak));
    }

    // 击中避雷针
    @WrapOperation(method = "hitBlockEnchantmentEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;onHitBlock(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/block/state/BlockState;Ljava/util/function/Consumer;)V"))
    private void onHitBlock(ServerLevel serverLevel, ItemStack weapon, @Nullable LivingEntity owner, Entity entity, @Nullable EquipmentSlot slot, Vec3 hitLocation, BlockState hitBlock, Consumer<Item> onBreak, Operation<Void> original) {
        ScopedValue.where(CarpetOrgAdditionSettings.USE_CHANNELING_TRIDENT, true)
                .call(() -> original.call(serverLevel, weapon, owner, entity, slot, hitLocation, hitBlock, onBreak));
    }
}
