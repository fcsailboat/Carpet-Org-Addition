package org.carpetorgaddition.mixin.util;

import net.minecraft.server.network.ServerPlayerEntity;
import org.carpetorgaddition.periodic.PeriodicTaskManagerInterface;
import org.carpetorgaddition.periodic.PlayerPeriodicTaskManager;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin implements PeriodicTaskManagerInterface {
    @Unique
    private final ServerPlayerEntity thisPlayer = (ServerPlayerEntity) (Object) this;
    @Unique
    @NotNull
    private final PlayerPeriodicTaskManager manager = new PlayerPeriodicTaskManager(thisPlayer);

    @Override
    public PlayerPeriodicTaskManager carpet_Org_Addition$getPlayerPeriodicTaskManager() {
        return this.manager;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tick(CallbackInfo ci) {
        this.manager.tick();
    }

    @Inject(method = "copyFrom", at = @At("HEAD"))
    private void copyFrom(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        this.manager.copyFrom(oldPlayer);
    }
}
