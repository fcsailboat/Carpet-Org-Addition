package boat.carpetorgaddition.mixin.util;

import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.FakePlayerSpawner;
import boat.carpetorgaddition.wheel.inventory.FabricPlayerAccessManager;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(PlayerList.class)
public class PlayerManagerMixin {
    // 隐藏玩家登录登出的消息
    @Inject(method = "broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V", at = @At("HEAD"), cancellable = true)
    private void broadcast(Component message, boolean overlay, CallbackInfo ci) {
        if (FakePlayerSpawner.SILENCE.orElse(false)) {
            ci.cancel();
        }
    }

    @WrapWithCondition(method = "placeNewPlayer", at = @At(value = "INVOKE", remap = false, target = "Lorg/slf4j/Logger;info(Ljava/lang/String;[Ljava/lang/Object;)V"))
    private boolean hide(Logger instance, String s, Object[] objects) {
        return !FakePlayerSpawner.SILENCE.orElse(false);
    }

    /**
     * 如果被打开物品栏的玩家在物品栏被打开的期间上线，则自动关闭打开物品栏玩家的GUI
     */
    @Inject(method = "placeNewPlayer", at = @At("HEAD"))
    private void closePlayerInventory(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        MinecraftServer server = ServerUtils.getServer(player);
        ServerComponentCoordinator coordinator = ServerComponentCoordinator.getCoordinator(server);
        FabricPlayerAccessManager accessManager = coordinator.getAccessManager();
        if (accessManager.hasViewers()) {
            NameAndId entry = player.nameAndId();
            Set<ServerPlayer> viewers = accessManager.getViewers(entry);
            if (viewers.isEmpty()) {
                return;
            }
            for (ServerPlayer viewer : Set.copyOf(viewers)) {
                viewer.closeContainer();
            }
        }
    }
}
