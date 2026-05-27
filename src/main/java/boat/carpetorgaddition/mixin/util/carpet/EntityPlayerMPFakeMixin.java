package boat.carpetorgaddition.mixin.util.carpet;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.periodic.FakePlayerComponentCoordinator;
import boat.carpetorgaddition.periodic.fakeplayer.action.FakePlayerActionManager;
import boat.carpetorgaddition.wheel.FakePlayerSpawner;
import carpet.patches.EntityPlayerMPFake;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Relative;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Mixin(value = EntityPlayerMPFake.class)
public class EntityPlayerMPFakeMixin {
    @Unique
    private final EntityPlayerMPFake self = (EntityPlayerMPFake) (Object) this;

    @WrapOperation(method = "createFake", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;whenCompleteAsync(Ljava/util/function/BiConsumer;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"))
    private static <T> CompletableFuture<T> whenCompleteAsync(CompletableFuture<Optional<GameProfile>> instance, BiConsumer<? super T, ? super Throwable> action, Executor executor, Operation<CompletableFuture<T>> original) {
        if (FakePlayerSpawner.CALLBACK.isBound()) {
            Consumer<EntityPlayerMPFake> callback = FakePlayerSpawner.CALLBACK.get();
            BiConsumer<? super T, Throwable> biConsumer = (value, throwable) -> ScopedValue
                    .where(FakePlayerSpawner.CALLBACK, callback)
                    .run(() -> action.accept(value, throwable));
            return original.call(instance, biConsumer, executor);
        } else {
            return original.call(instance, action, executor);
        }
    }

    @WrapOperation(method = "createFake", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;whenCompleteAsync(Ljava/util/function/BiConsumer;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"))
    private static <T> CompletableFuture<T> fakePlayerLoginMessage(CompletableFuture<T> instance, BiConsumer<? super T, ? super Throwable> action, Executor executor, Operation<CompletableFuture<T>> original) {
        boolean hiddenMessage = FakePlayerSpawner.SILENCE.orElse(false);
        BiConsumer<? super T, ? super Throwable> consumer = (value, throwable) ->
                ScopedValue.where(FakePlayerSpawner.SILENCE, hiddenMessage)
                        .run(() -> action.accept(value, throwable));
        return original.call(instance, consumer, executor);
    }

    @Inject(method = "lambda$createFake$0", at = @At(value = "INVOKE", target = "Lcarpet/patches/EntityPlayerMPFake;getAbilities()Lnet/minecraft/world/entity/player/Abilities;"))
    private static void spawn(CallbackInfo ci, @Local(name = "instance") EntityPlayerMPFake instance) {
        if (FakePlayerSpawner.CALLBACK.isBound()) {
            FakePlayerSpawner.CALLBACK.get().accept(instance);
        }
    }

    @WrapOperation(method = "lambda$createFake$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/network/CommonListenerCookie;)V"))
    private static void onPlayerConnect(PlayerList instance, Connection connection, ServerPlayer player, CommonListenerCookie cookie, Operation<Void> original) {
        try {
            original.call(instance, connection, player, cookie);
        } catch (NullPointerException e) {
            if (FakePlayerSpawner.SILENCE.orElse(false)) {
                // 玩家在服务器关闭后登录游戏可能导致服务器崩溃（一般发生在服务器关闭时，有玩家的周期性上下线未停止）
                CarpetOrgAddition.LOGGER.warn("Fake player attempts to join game after server shutdown", e);
            } else {
                throw e;
            }
        }
    }

    @WrapOperation(method = "createFake", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;whenCompleteAsync(Ljava/util/function/BiConsumer;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"))
    private static <T> CompletableFuture<T> originalPositionSpawn(CompletableFuture<T> instance, BiConsumer<? super T, ? super Throwable> action, Executor executor, Operation<CompletableFuture<T>> original) {
        Boolean originalPosition = FakePlayerSpawner.ORIGINAL_POSITION.orElse(false);
        BiConsumer<? super T, ? super Throwable> consumer = (value, throwable) ->
                ScopedValue.where(FakePlayerSpawner.ORIGINAL_POSITION, originalPosition)
                        .run(() -> action.accept(value, throwable));
        return original.call(instance, consumer, executor);
    }

    @WrapOperation(method = "lambda$createFake$0", at = @At(value = "INVOKE", target = "Lcarpet/patches/EntityPlayerMPFake;teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FFZ)Z"))
    private static boolean originalPositionSpawn(EntityPlayerMPFake instance, ServerLevel serverWorld, double x, double y, double z, Set<Relative> set, float yaw, float pitch, boolean b, Operation<Boolean> original) {
        if (FakePlayerSpawner.ORIGINAL_POSITION.orElse(false)) {
            return false;
        }
        return original.call(instance, serverWorld, x, y, z, set, yaw, pitch, b);
    }

    @WrapOperation(method = "lambda$createFake$1", at = @At(value = "INVOKE", target = "Lcarpet/patches/EntityPlayerMPFake;snapTo(DDDFF)V"))
    private static void originalPositionSpawn(EntityPlayerMPFake instance, double x, double y, double z, float yaw, float pitch, Operation<Void> original) {
        if (FakePlayerSpawner.ORIGINAL_POSITION.orElse(false)) {
            return;
        }
        original.call(instance, x, y, z, yaw, pitch);
    }

    @WrapOperation(method = "kill(Lnet/minecraft/network/chat/Component;)V", at = @At(value = "NEW", target = "(ILjava/lang/Runnable;)Lnet/minecraft/server/TickTask;"))
    private TickTask kill(int tick, Runnable runnable, Operation<TickTask> original) {
        boolean silence = FakePlayerSpawner.SILENCE.orElse(false);
        return original.call(tick, (Runnable) () -> ScopedValue.where(FakePlayerSpawner.SILENCE, silence).run(runnable));
    }

    @WrapWithCondition(method = "lambda$createFake$0", at = @At(value = "INVOKE", target = "Lcarpet/patches/EntityPlayerMPFake;stopRiding()V"))
    private static boolean stopRiding(EntityPlayerMPFake instance) {
        return !FakePlayerSpawner.ORIGINAL_POSITION.orElse(false);
    }

    @Inject(method = "kill(Lnet/minecraft/network/chat/Component;)V", at = @At("HEAD"))
    private void kill(Component reason, CallbackInfo ci) {
        FakePlayerComponentCoordinator coordinator = FakePlayerComponentCoordinator.getCoordinator(this.self);
        FakePlayerActionManager actionManager = coordinator.getFakePlayerActionManager();
        actionManager.getAction().onFakePlayerLogout();
        actionManager.stop();
    }
}
