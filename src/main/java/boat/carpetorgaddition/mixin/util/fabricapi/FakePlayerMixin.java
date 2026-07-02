package boat.carpetorgaddition.mixin.util.fabricapi;

import boat.carpetorgaddition.wheel.inventory.FabricPlayerAccessor;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FakePlayer.class)
public class FakePlayerMixin extends ServerPlayer {
    public FakePlayerMixin(MinecraftServer server, ServerLevel level, GameProfile gameProfile, ClientInformation clientInformation) {
        super(server, level, gameProfile, clientInformation);
    }

    @Inject(method = "startRiding", at = @At("HEAD"), cancellable = true)
    private void startRiding(Entity entity, boolean force, boolean emitEvent, CallbackInfoReturnable<Boolean> cir) {
        if (FabricPlayerAccessor.INIT_FABRIC_PLAYER.orElse(false)) {
            cir.setReturnValue(super.startRiding(entity, force, emitEvent));
        }
    }
}
