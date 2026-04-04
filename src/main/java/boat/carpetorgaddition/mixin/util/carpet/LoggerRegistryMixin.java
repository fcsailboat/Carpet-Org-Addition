package boat.carpetorgaddition.mixin.util.carpet;

import boat.carpetorgaddition.logger.Loggers;
import boat.carpetorgaddition.util.ServerUtils;
import carpet.logging.LoggerRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(value = LoggerRegistry.class, remap = false)
public class LoggerRegistryMixin {
    // 记录器取消订阅事件
    @Inject(method = "unsubscribePlayer", at = @At(value = "INVOKE", target = "Ljava/util/Map;size()I"))
    private static void unsubscribePlayer(String playerName, String logName, CallbackInfo ci) {
        Optional<MinecraftServer> server = ServerUtils.getCurrentServer();
        if (server.isEmpty()) {
            return;
        }
        ServerPlayer player = server.get().getPlayerList().getPlayerByName(playerName);
        if (player != null) {
            Loggers.getLogger(logName).ifPresent(accessor -> accessor.onUnsubscribe(player));
        }
    }
}
