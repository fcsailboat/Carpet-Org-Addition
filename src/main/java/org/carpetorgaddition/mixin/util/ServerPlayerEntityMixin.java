package org.carpetorgaddition.mixin.util;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.world.GameMode;
import org.carpetorgaddition.network.s2c.BackgroundSpriteSyncS2CPacket;
import org.carpetorgaddition.network.s2c.UnavailableSlotSyncS2CPacket;
import org.carpetorgaddition.periodic.PeriodicTaskManagerInterface;
import org.carpetorgaddition.periodic.PlayerComponentCoordinator;
import org.carpetorgaddition.wheel.screen.BackgroundSpriteSyncServer;
import org.carpetorgaddition.wheel.screen.UnavailableSlotSyncInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalInt;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin implements PeriodicTaskManagerInterface {
    @Unique
    private final ServerPlayerEntity thisPlayer = (ServerPlayerEntity) (Object) this;
    @Unique
    private PlayerComponentCoordinator manager;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(MinecraftServer server, ServerWorld world, GameProfile profile, SyncedClientOptions clientOptions, CallbackInfo ci) {
        this.manager = PlayerComponentCoordinator.of(thisPlayer);
    }

    @Override
    public PlayerComponentCoordinator carpet_Org_Addition$getPlayerPeriodicTaskManager() {
        return this.manager;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tick(CallbackInfo ci) {
        this.manager.tick();
    }

    @Inject(method = "copyFrom", at = @At("HEAD"))
    private void copyFrom(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        this.manager.copyFrom(oldPlayer);
    }

    @Inject(method = "openHandledScreen", at = @At(value = "RETURN", ordinal = 2))
    private void openHandledScreen(NamedScreenHandlerFactory factory, CallbackInfoReturnable<OptionalInt> cir, @Local ScreenHandler screenHandler) {
        // 同步不可用槽位
        if (screenHandler instanceof UnavailableSlotSyncInterface anInterface) {
            ServerPlayNetworking.send(thisPlayer, new UnavailableSlotSyncS2CPacket(screenHandler.syncId, anInterface.from(), anInterface.to()));
        }
        // 同步槽位背景纹理
        if (screenHandler instanceof BackgroundSpriteSyncServer anInterface) {
            anInterface.getBackgroundSprite().forEach((index, identifier) ->
                    ServerPlayNetworking.send(thisPlayer, new BackgroundSpriteSyncS2CPacket(screenHandler.syncId, index, identifier)));
        }
    }

    @WrapOperation(method = "readCustomData", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;gameModeFromData(Lnet/minecraft/storage/ReadView;Ljava/lang/String;)Lnet/minecraft/world/GameMode;", ordinal = 0))
    private GameMode setNbtGameMode(ReadView view, String key, Operation<GameMode> original) {
        GameMode gameMode = original.call(view, key);
        this.manager.setNbtGameMode(gameMode);
        return gameMode;
    }
}
