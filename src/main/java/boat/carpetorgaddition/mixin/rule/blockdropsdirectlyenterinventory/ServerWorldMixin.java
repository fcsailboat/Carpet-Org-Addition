package boat.carpetorgaddition.mixin.rule.blockdropsdirectlyenterinventory;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public class ServerWorldMixin {
    @Inject(method = "addEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/PersistentEntitySectionManager;addNewEntity(Lnet/minecraft/world/level/entity/EntityAccess;)Z"))
    private void collect(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (CarpetOrgAdditionSettings.BLOCK_BREAKER.isBound() && entity instanceof ItemEntity itemEntity) {
            ServerPlayer player = CarpetOrgAdditionSettings.BLOCK_BREAKER.get();
            if (CarpetOrgAdditionSettings.BLOCK_DROPS_DIRECTLY_ENTER_INVENTORY.value(player).isEnabled()) {
                Inventory inventory = player.getInventory();
                ItemStack itemStack = itemEntity.getItem();
                inventory.add(itemStack);
            }
        }
    }
}
