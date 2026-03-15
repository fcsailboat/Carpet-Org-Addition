package boat.carpetorgaddition.mixin.rule;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Containers.class)
public class ItemScattererMixin {
    @Inject(method = "dropItemStack(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V", at = @At(value = "HEAD"), cancellable = true)
    private static void onStateReplaced(Level world, double x, double y, double z, ItemStack stack, CallbackInfo ci) {
        if (CarpetOrgAdditionSettings.DISABLE_CREATIVE_CONTAINER_DROPS.value()) {
            ServerPlayer player = CarpetOrgAdditionSettings.BLOCK_BREAKER.get();
            if (player != null && player.isCreative()) {
                ci.cancel();
            }
        }
    }
}
