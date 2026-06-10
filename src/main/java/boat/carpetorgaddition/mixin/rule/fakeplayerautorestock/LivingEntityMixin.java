package boat.carpetorgaddition.mixin.rule.fakeplayerautorestock;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.periodic.fakeplayer.action.FakePlayerActionManager;
import boat.carpetorgaddition.util.InventoryUtils;
import boat.carpetorgaddition.wheel.inventory.PlayerStorageInventory;
import carpet.patches.EntityPlayerMPFake;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow
    public abstract ItemStack getItemInHand(InteractionHand hand);

    @Unique
    private ItemStack useItemCopy = null;

    @Inject(method = "completeUsingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;finishUsingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"))
    private void beforeCompletion(CallbackInfo ci, @Local(name = "hand") InteractionHand hand) {
        this.useItemCopy = this.getItemInHand(hand).copy();
    }

    @WrapMethod(method = "completeUsingItem")
    private void afterCompletion(Operation<Void> original) {
        try {
            original.call();
        } finally {
            this.useItemCopy = null;
        }
    }

    @WrapOperation(method = "completeUsingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setItemInHand(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;)V"))
    private void completeUsingItem(LivingEntity instance, InteractionHand hand, ItemStack itemStack, Operation<Void> original) {
        if (instance instanceof EntityPlayerMPFake fakePlayer) {
            ItemStack copy = this.useItemCopy;
            if (copy != null && this.restock(copy, fakePlayer.getItemInHand(hand), hand, fakePlayer)) {
                PlayerStorageInventory inventory = PlayerStorageInventory.of(fakePlayer);
                inventory.insertWithInventoryPriority(itemStack);
                return;
            }
        }
        original.call(instance, hand, itemStack);
    }

    @Unique
    private boolean restock(ItemStack copy, ItemStack itemStack, InteractionHand hand, EntityPlayerMPFake fakePlayer) {
        if (FakePlayerActionManager.IN_ACTION.orElse(false)) {
            // 玩家动作的补货，交给相应的动作自行处理
            return false;
        }
        if (CarpetOrgAdditionSettings.FAKE_PLAYER_AUTO_RESTOCK.value()) {
            PlayerStorageInventory inventory = PlayerStorageInventory.of(fakePlayer);
            if (InventoryUtils.isFragileWithMending(itemStack)) {
                inventory.replenish(hand, stack -> stack.is(itemStack.getItem()) && !InventoryUtils.isFragileWithMending(stack));
            } else if (itemStack.isEmpty()) {
                inventory.replenish(hand, stack -> InventoryUtils.canMerge(copy, stack));
            } else {
                inventory.replenish(hand, Math.max(1, itemStack.getMaxStackSize() / 2));
            }
            return true;
        }
        return false;
    }
}
