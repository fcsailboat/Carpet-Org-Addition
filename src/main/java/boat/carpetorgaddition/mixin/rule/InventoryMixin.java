package boat.carpetorgaddition.mixin.rule;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Inventory.class)
public class InventoryMixin {
    @WrapOperation(method = "dropAll", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private ItemEntity drop(Player instance, ItemStack itemStack, boolean randomly, boolean thrownFromHand, Operation<ItemEntity> original) {
        ItemEntity itemEntity = original.call(instance, itemStack, randomly, thrownFromHand);
        if (CarpetOrgAdditionSettings.PLAYER_DROPS_NOT_DESPAWNING.value() && itemEntity != null) {
            itemEntity.setUnlimitedLifetime();
        }
        return itemEntity;
    }
}
