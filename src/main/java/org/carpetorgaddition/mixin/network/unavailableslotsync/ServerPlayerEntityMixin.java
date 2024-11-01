package org.carpetorgaddition.mixin.network.unavailableslotsync;

import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.carpetorgaddition.network.s2c.UnavailableSlotSyncS2CPacket;
import org.carpetorgaddition.util.screen.UnavailableSlotSyncInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalInt;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
    @Unique
    private final ServerPlayerEntity thisPlayer = (ServerPlayerEntity) (Object) this;

    @Inject(method = "openHandledScreen", at = @At(value = "RETURN", ordinal = 2))
    private void openHandledScreen(NamedScreenHandlerFactory factory, CallbackInfoReturnable<OptionalInt> cir, @Local ScreenHandler screenHandler) {
        if (screenHandler instanceof UnavailableSlotSyncInterface anInterface) {
            ServerPlayNetworking.send(thisPlayer, new UnavailableSlotSyncS2CPacket(screenHandler.syncId, anInterface.from(), anInterface.to()));
        }
    }
}
