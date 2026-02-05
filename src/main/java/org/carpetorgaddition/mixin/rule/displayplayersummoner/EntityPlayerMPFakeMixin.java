package org.carpetorgaddition.mixin.rule.displayplayersummoner;

import carpet.patches.EntityPlayerMPFake;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.util.FetcherUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.wheel.TextBuilder;
import org.carpetorgaddition.wheel.provider.TextProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

@Mixin(value = EntityPlayerMPFake.class)
public class EntityPlayerMPFakeMixin {
    @WrapOperation(method = "createFake", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;whenCompleteAsync(Ljava/util/function/BiConsumer;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;", remap = false))
    private static <T> CompletableFuture<T> thenAcceptAsync(CompletableFuture<T> instance, BiConsumer<? super T, ? super Throwable> action, Executor executor, Operation<CompletableFuture<T>> original) {
        ServerPlayerEntity player = CarpetOrgAdditionSettings.playerSummoner.get();
        BiConsumer<? super T, ? super Throwable> consumer = (value, throwable) -> {
            try {
                CarpetOrgAdditionSettings.internalPlayerSummoner.set(player);
                action.accept(value, throwable);
            } finally {
                CarpetOrgAdditionSettings.internalPlayerSummoner.remove();
            }
        };
        return original.call(instance, consumer, executor);
    }

    @WrapOperation(method = "lambda$createFake$2", at = @At(value = "INVOKE", target = "Lcarpet/patches/EntityPlayerMPFake;getAbilities()Lnet/minecraft/entity/player/PlayerAbilities;"))
    private static PlayerAbilities broadcastSummoner(EntityPlayerMPFake instance, Operation<PlayerAbilities> original) {
        broadcastSummoner(instance);
        return original.call(instance);
    }

    @Unique
    private static void broadcastSummoner(EntityPlayerMPFake fakePlayer) {
        if (CarpetOrgAdditionSettings.displayPlayerSummoner.get()) {
            ServerPlayerEntity player = CarpetOrgAdditionSettings.internalPlayerSummoner.get();
            if (player == null || CarpetOrgAdditionSettings.hiddenLoginMessages.getInternal()) {
                return;
            }
            TextBuilder builder = TextBuilder.of("carpet.rule.message.displayPlayerSummoner", player.getDisplayName());
            builder.setGrayItalic();
            Text dimension = TextProvider.dimension(FetcherUtils.getWorld(fakePlayer));
            Text blockPos = TextProvider.simpleBlockPos(fakePlayer.getBlockPos());
            Text pos = TextBuilder.combineAll(dimension, ": ", blockPos);
            builder.setHover(pos);
            MessageUtils.broadcastMessage(FetcherUtils.getServer(player), builder.build());
            CarpetOrgAddition.LOGGER.info("{} has summoned {} at {}", FetcherUtils.getPlayerName(player), FetcherUtils.getPlayerName(fakePlayer), pos.getString());
        }
    }
}
