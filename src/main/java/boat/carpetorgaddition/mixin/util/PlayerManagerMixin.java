package boat.carpetorgaddition.mixin.util;

import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.periodic.task.schedule.ReLoginTask;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.FakePlayerSpawner;
import boat.carpetorgaddition.wheel.inventory.FabricPlayerAccessManager;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(PlayerList.class)
public class PlayerManagerMixin {
    @Shadow
    @Final
    private MinecraftServer server;

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

    @WrapWithCondition(method = "remove", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastAll(Lnet/minecraft/network/protocol/Packet;)V"))
    private boolean remove(PlayerList instance, Packet<?> packet, @Local(argsOnly = true, name = "player") ServerPlayer player) {
        // 如果当前玩家正在进行重复上下线任务，则不向客户端发送玩家退出的数据包，避免玩家列表闪烁
        return ServerComponentCoordinator.getCoordinator(this.server)
                .getServerTaskManager()
                .stream(ReLoginTask.class)
                .noneMatch(reLoginTask -> reLoginTask.getUuid().equals(player.getUUID()));
    }
}
