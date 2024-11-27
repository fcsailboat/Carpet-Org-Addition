package org.carpetorgaddition.mixin.event.carpet;

import carpet.CarpetServer;
import carpet.logging.LoggerRegistry;
import net.minecraft.server.network.ServerPlayerEntity;
import org.carpetorgaddition.event.LoggerSubscribeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LoggerRegistry.class, remap = false)
public class LoggerRegistryMixin {
    // 记录器订阅事件
    @Inject(method = "subscribePlayer", at = @At(value = "RETURN"))
    private static void subscribePlayer(String playerName, String logName, String option, CallbackInfo ci) {
        if (CarpetServer.minecraft_server == null) {
            return;
        }
        ServerPlayerEntity player = CarpetServer.minecraft_server.getPlayerManager().getPlayer(playerName);
        if (player != null) {
            LoggerSubscribeEvent.SUBSCRIBE.invoker().accept(player, logName, option);
        }
    }

    // 记录器取消订阅事件
    @Inject(method = "unsubscribePlayer", at = @At(value = "INVOKE", target = "Ljava/util/Map;size()I"))
    private static void unsubscribePlayer(String playerName, String logName, CallbackInfo ci) {
        if (CarpetServer.scriptServer == null) {
            return;
        }
        ServerPlayerEntity player = CarpetServer.minecraft_server.getPlayerManager().getPlayer(playerName);
        if (player != null) {
            LoggerSubscribeEvent.UNSUBSCRIBE.invoker().accept(player, logName);
        }
    }
}
