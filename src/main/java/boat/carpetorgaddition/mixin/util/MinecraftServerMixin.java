package boat.carpetorgaddition.mixin.util;

import boat.carpetorgaddition.periodic.PeriodicTaskManagerInterface;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.wheel.ServerConfigOneShotLatch;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin implements PeriodicTaskManagerInterface, ServerConfigOneShotLatch {
    @Unique
    private ServerComponentCoordinator coordinator;
    @Unique
    private boolean available = true;

    @Inject(method = "tickServer", at = @At("HEAD"))
    private void tick(BooleanSupplier haveTime, CallbackInfo ci) {
        if (this.coordinator != null) {
            this.coordinator.tick();
        }
    }

    @Override
    public ServerComponentCoordinator carpet_Org_Addition$getServerComponentCoordinator() {
        return this.coordinator;
    }

    @Override
    public void carpet_Org_Addition$setServerComponentCoordinator(ServerComponentCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @Override
    public boolean carpet_Org_Addition$tryAcquire() {
        if (this.available) {
            this.available = false;
            return true;
        }
        return false;
    }
}
