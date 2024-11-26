package org.carpetorgaddition.mixin.event;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.carpetorgaddition.event.PlayerOpenHandledScreenEvent;
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
        PlayerOpenHandledScreenEvent.ALREADY_OPENED.invoker().after(thisPlayer, screenHandler);
    }
}
