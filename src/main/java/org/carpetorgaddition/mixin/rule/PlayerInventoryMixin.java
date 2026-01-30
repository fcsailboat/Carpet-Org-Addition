package org.carpetorgaddition.mixin.rule;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin {
    @WrapOperation(method = "dropAll",at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;"))
    private ItemEntity dropAll(PlayerEntity instance, ItemStack itemStack, boolean dropAtSelf, boolean retainOwnership, Operation<ItemEntity> original) {
        ItemEntity itemEntity = original.call(instance, itemStack, dropAtSelf, retainOwnership);
        if (CarpetOrgAdditionSettings.playerDropsNotDespawning.get() && itemEntity != null) {
            // 设置掉落物不消失
            itemEntity.setNeverDespawn();
        }
        return itemEntity;
    }
}
