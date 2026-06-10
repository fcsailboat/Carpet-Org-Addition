package boat.carpetorgaddition.mixin.rule.fakeplayerautorestock;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.periodic.fakeplayer.action.FakePlayerActionManager;
import boat.carpetorgaddition.util.InventoryUtils;
import boat.carpetorgaddition.wheel.inventory.PlayerStorageInventory;
import carpet.patches.EntityPlayerMPFake;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @Unique
    private static final ScopedValue<ItemStack> SELF_COPY = ScopedValue.newInstance();

    @WrapOperation(method = "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V"))
    private void hurtAndBreak(ItemStack instance, int amount, ServerLevel level, @Nullable ServerPlayer player, Consumer<Item> onBreak, Operation<Void> original) {
        ScopedValue.where(SELF_COPY, ((ItemStack) (Object) this).copy()).run(() -> original.call(instance, amount, level, player, onBreak));
    }

    @WrapOperation(method = "lambda$hurtAndBreak$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;onEquippedItemBroken(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/entity/EquipmentSlot;)V"))
    private static void restock(LivingEntity instance, Item brokenItem, EquipmentSlot inSlot, Operation<Void> original, @Local(argsOnly = true, name = "owner") LivingEntity owner) {
        original.call(instance, brokenItem, inSlot);
        if (FakePlayerActionManager.IN_ACTION.orElse(false)) {
            return;
        }
        if (CarpetOrgAdditionSettings.FAKE_PLAYER_AUTO_RESTOCK.value() && owner instanceof EntityPlayerMPFake fakePlayer && SELF_COPY.isBound()) {
            ItemStack selfCopy = SELF_COPY.get();
            PlayerStorageInventory inventory = PlayerStorageInventory.of(fakePlayer);
            InteractionHand hand = inSlot == EquipmentSlot.MAINHAND ? InteractionHand.MAIN_HAND : (inSlot == EquipmentSlot.OFFHAND ? InteractionHand.OFF_HAND : null);
            if (hand == null) {
                return;
            }
            inventory.replenish(hand, itemStack -> itemStack.is(selfCopy.getItem()) && !InventoryUtils.isFragileWithMending(itemStack));
        }
    }
}
