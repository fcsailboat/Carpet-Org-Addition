package boat.carpetorgaddition.mixin.rule.notoolbreak;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.rule.RuleUtils;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Holder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BiConsumer;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Unique
    private final LivingEntity self = (LivingEntity) (Object) this;

    @Inject(method = "getItemInHand", at = @At("HEAD"), cancellable = true)
    private void getItemInHand(InteractionHand hand, CallbackInfoReturnable<ItemStack> cir) {
        if (this.self instanceof Player && CarpetOrgAdditionSettings.USE_TOOL_ITEM_NO_BREAK.orElse(false)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }

    @Inject(method = "getMainHandItem", at = @At("HEAD"), cancellable = true)
    private void getMainHandItem(CallbackInfoReturnable<ItemStack> cir) {
        if (this.self instanceof Player && CarpetOrgAdditionSettings.USE_TOOL_ITEM_NO_BREAK.orElse(false)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }

    @WrapOperation(method = "collectEquipmentChanges", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V"))
    private void collectEquipmentChanges(ItemStack instance, EquipmentSlot slot, BiConsumer<Holder<Attribute>, AttributeModifier> consumer, Operation<Void> original) {
        boolean noBreak = this.self instanceof Player player && RuleUtils.isToolNoBreak(instance, player);
        original.call(noBreak ? ItemStack.EMPTY : instance, slot, consumer);
    }

    @WrapOperation(method = "stopLocationBasedEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V"))
    private void stopLocationBasedEffects(ItemStack instance, EquipmentSlot slot, BiConsumer<Holder<Attribute>, AttributeModifier> consumer, Operation<Void> original) {
        boolean noBreak = this.self instanceof Player player && RuleUtils.isToolNoBreak(instance, player);
        original.call(noBreak ? ItemStack.EMPTY : instance, slot, consumer);
    }
}
