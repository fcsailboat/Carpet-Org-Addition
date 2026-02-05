package org.carpetorgaddition.mixin.util.carpet;

import carpet.patches.EntityPlayerMPFake;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.periodic.task.batch.BatchSpawnFakePlayerTask;
import org.carpetorgaddition.periodic.task.schedule.ReLoginTask;
import org.carpetorgaddition.util.GenericUtils;
import org.carpetorgaddition.wheel.ThreadContextPropagator;
import org.spongepowered.asm.mixin.Mixin;
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
    @WrapOperation(method = "createFake", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;whenCompleteAsync(Ljava/util/function/BiConsumer;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"))
    private static <T> CompletableFuture<T> whenCompleteAsync(CompletableFuture<Optional<GameProfile>> instance, BiConsumer<? super T, ? super Throwable> action, Executor executor, Operation<CompletableFuture<T>> original) {
        Consumer<EntityPlayerMPFake> onFakePlayerSpawning = GenericUtils.FAKE_PLAYER_SPAWNING.get();
        if (onFakePlayerSpawning == null) {
            return original.call(instance, action, executor);
        }
        BiConsumer<? super T, Throwable> biConsumer = (value, throwable) -> {
            try {
                GenericUtils.INTERNAL_FAKE_PLAYER_SPAWNING.set(onFakePlayerSpawning);
                action.accept(value, throwable);
            } finally {
                GenericUtils.INTERNAL_FAKE_PLAYER_SPAWNING.remove();
            }
        };
        return original.call(instance, biConsumer, executor);
    }

    @WrapOperation(method = "createFake", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;whenCompleteAsync(Ljava/util/function/BiConsumer;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"))
    private static <T> CompletableFuture<T> fakePlayerLoginMessage(CompletableFuture<T> instance, BiConsumer<? super T, ? super Throwable> action, Executor executor, Operation<CompletableFuture<T>> original) {
        ThreadContextPropagator<Boolean> propagator = CarpetOrgAdditionSettings.hiddenLoginMessages;
        Boolean external = propagator.getExternal();
        Boolean hiddenBatchSpawn = BatchSpawnFakePlayerTask.batchSpawnHiddenMessage.get();
        BiConsumer<? super T, ? super Throwable> consumer = (value, throwable) -> {
            try {
                BatchSpawnFakePlayerTask.internalBatchSpawnHiddenMessage.set(hiddenBatchSpawn);
                propagator.setInternal(external);
                action.accept(value, throwable);
            } finally {
                propagator.setInternal(false);
                BatchSpawnFakePlayerTask.internalBatchSpawnHiddenMessage.set(false);
            }
        };
        return original.call(instance, consumer, executor);
    }

    @Inject(method = "lambda$createFake$2", at = @At(value = "INVOKE", target = "Lcarpet/patches/EntityPlayerMPFake;getAbilities()Lnet/minecraft/entity/player/PlayerAbilities;"))
    private static void spawn(CallbackInfo ci, @Local EntityPlayerMPFake fakePlayer) {
        Consumer<EntityPlayerMPFake> consumer = GenericUtils.INTERNAL_FAKE_PLAYER_SPAWNING.get();
        if (consumer == null) {
            return;
        }
        consumer.accept(fakePlayer);
    }

    @WrapOperation(method = "lambda$createFake$2", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/server/network/ConnectedClientData;)V"))
    private static void onPlayerConnect(PlayerManager instance, ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, Operation<Void> original, @Local EntityPlayerMPFake fakePlayer) {
        boolean internal = CarpetOrgAdditionSettings.hiddenLoginMessages.getInternal();
        try {
            original.call(instance, connection, player, clientData);
        } catch (NullPointerException e) {
            if (internal) {
                // 玩家在服务器关闭后登录游戏可能导致服务器崩溃
                CarpetOrgAddition.LOGGER.warn("Fake player attempts to join game after server shutdown", e);
            } else {
                throw e;
            }
        }
    }

    @WrapOperation(method = "createFake", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;whenCompleteAsync(Ljava/util/function/BiConsumer;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;"))
    private static <T> CompletableFuture<T> homePositionSpawn(CompletableFuture<T> instance, BiConsumer<? super T, ? super Throwable> action, Executor executor, Operation<CompletableFuture<T>> original) {
        Boolean shouldHomePosition = ReLoginTask.HOME_POSITION.get();
        BiConsumer<? super T, ? super Throwable> consumer = (value, throwable) -> {
            try {
                ReLoginTask.INTERNAL_HOME_POSITION.set(shouldHomePosition);
                action.accept(value, throwable);
            } finally {
                ReLoginTask.INTERNAL_HOME_POSITION.set(false);
            }
        };
        return original.call(instance, consumer, executor);
    }

    @WrapOperation(method = "lambda$createFake$2", at = @At(value = "INVOKE", target = "Lcarpet/patches/EntityPlayerMPFake;teleport(Lnet/minecraft/server/world/ServerWorld;DDDLjava/util/Set;FFZ)Z"))
    private static boolean homePositionSpawn(EntityPlayerMPFake instance, ServerWorld serverWorld, double x, double y, double z, Set<PositionFlag> set, float yaw, float pitch, boolean b, Operation<Boolean> original) {
        if (ReLoginTask.INTERNAL_HOME_POSITION.get()) {
            return false;
        }
        return original.call(instance, serverWorld, x, y, z, set, yaw, pitch, b);
    }
}
