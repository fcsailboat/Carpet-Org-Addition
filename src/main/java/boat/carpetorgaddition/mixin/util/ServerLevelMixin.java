package boat.carpetorgaddition.mixin.util;

import boat.carpetorgaddition.wheel.inventory.FabricPlayerAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {
    @Inject(method = "addWithUUID", at = @At("HEAD"), cancellable = true)
    private void addWithUUID(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (FabricPlayerAccessor.INIT_FABRIC_PLAYER.orElse(false)) {
            cir.setReturnValue(true);
        }
    }
}
