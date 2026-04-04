package boat.carpetorgaddition.mixin.logger;

import boat.carpetorgaddition.wheel.misc.WanderingTraderSpawnData;
import carpet.logging.HUDController;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(HUDController.class)
public class HUDControllerMixin {
    @Inject(method = "update_hud", at = @At(value = "INVOKE", target = "Ljava/util/Map;keySet()Ljava/util/Set;"), remap = false)
    private static void updateHud(MinecraftServer server, List<ServerPlayer> force, CallbackInfo ci) {
        WanderingTraderSpawnData.updateHud(server);
    }
}
