package boat.carpetorgaddition.mixin.rule.displayplayersummoner;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.PlayerUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.FakePlayerSpawner;
import boat.carpetorgaddition.wheel.provider.TextProvider;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import carpet.patches.EntityPlayerMPFake;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Abilities;
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
        ServerPlayer player = CarpetOrgAdditionSettings.playerSummoner.get();
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

    @WrapOperation(method = "lambda$createFake$0", at = @At(value = "INVOKE", target = "Lcarpet/patches/EntityPlayerMPFake;getAbilities()Lnet/minecraft/world/entity/player/Abilities;"))
    private static Abilities broadcastSummoner(EntityPlayerMPFake instance, Operation<Abilities> original) {
        broadcastSummoner(instance);
        return original.call(instance);
    }

    @Unique
    private static void broadcastSummoner(EntityPlayerMPFake fakePlayer) {
        if (CarpetOrgAdditionSettings.displayPlayerSummoner.value()) {
            ServerPlayer player = CarpetOrgAdditionSettings.internalPlayerSummoner.get();
            if (player == null || FakePlayerSpawner.SILENCE.orElse(false)) {
                return;
            }
            TextBuilder builder = LocalizationKeys.Rule.Message.DISPLAY_PLAYER_SUMMONER.builder(player.getDisplayName());
            builder.setGrayItalic();
            Component dimension = TextProvider.dimension(ServerUtils.getWorld(fakePlayer));
            Component blockPos = TextProvider.simpleBlockPos(fakePlayer.blockPosition());
            Component pos = TextBuilder.combineAll(dimension, ": ", blockPos);
            builder.setHover(pos);
            MessageUtils.sendMessage(ServerUtils.getServer(player), builder.build());
            CarpetOrgAddition.LOGGER.info("{} has summoned {} at {}", PlayerUtils.getName(player), PlayerUtils.getName(fakePlayer), pos.getString());
        }
    }
}
