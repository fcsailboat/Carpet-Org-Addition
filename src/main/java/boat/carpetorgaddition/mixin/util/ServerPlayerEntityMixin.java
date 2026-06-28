package boat.carpetorgaddition.mixin.util;

import boat.carpetorgaddition.network.s2c.BackgroundSpriteSyncS2CPacket;
import boat.carpetorgaddition.network.s2c.OldPlayerInventoryScreenSyncS2CPacket;
import boat.carpetorgaddition.network.s2c.UnavailableSlotSyncS2CPacket;
import boat.carpetorgaddition.network.s2c.WithButtonScreenSyncS2CPacket;
import boat.carpetorgaddition.periodic.PeriodicTaskManagerInterface;
import boat.carpetorgaddition.periodic.PlayerComponentCoordinator;
import boat.carpetorgaddition.util.PlayerUtils;
import boat.carpetorgaddition.wheel.screen.AbstractPlayerInventoryScreenHandler;
import boat.carpetorgaddition.wheel.screen.BackgroundSpriteSyncServer;
import boat.carpetorgaddition.wheel.screen.UnavailableSlotSyncInterface;
import boat.carpetorgaddition.wheel.screen.WithButtonPlayerInventoryScreenHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.ValueInput;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.OptionalInt;

@Mixin(value = ServerPlayer.class, priority = 1001)
public class ServerPlayerEntityMixin implements PeriodicTaskManagerInterface {
    @Unique
    private final ServerPlayer self = (ServerPlayer) (Object) this;
    @Nullable
    @Unique
    private PlayerComponentCoordinator coordinator;

    @Override
    public PlayerComponentCoordinator carpet_Org_Addition$getPlayerPeriodicTaskManager() {
        return this.coordinator;
    }

    @Override
    public void carpet_Org_Addition$setPlayerComponentCoordinator(PlayerComponentCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tick(CallbackInfo ci) {
        if (this.coordinator != null) {
            this.coordinator.tick();
        }
    }

    @Inject(method = "restoreFrom", at = @At("HEAD"))
    private void copyFrom(ServerPlayer oldPlayer, boolean restoreAll, CallbackInfo ci) {
        if (this.coordinator != null) {
            this.coordinator.copyFrom(oldPlayer);
        }
    }

    @Inject(method = "openMenu", at = @At(value = "RETURN", ordinal = 2))
    private void openHandledScreen(MenuProvider provider, CallbackInfoReturnable<OptionalInt> cir, @Local(name = "menu") AbstractContainerMenu menu) {
        // 同步不可用槽位
        if (menu instanceof UnavailableSlotSyncInterface unavailable) {
            PlayerUtils.sendNetworkPacket(this.self, new UnavailableSlotSyncS2CPacket(menu.containerId, unavailable.from(), unavailable.to()));
        } else if (menu instanceof WithButtonPlayerInventoryScreenHandler) {
            PlayerUtils.sendNetworkPacket(this.self, new WithButtonScreenSyncS2CPacket(menu.containerId));
        }
        // 同步槽位背景纹理
        if (menu instanceof BackgroundSpriteSyncServer background) {
            background.getBackgroundSprite().forEach((index, identifier) ->
                    ServerPlayNetworking.send(this.self, new BackgroundSpriteSyncS2CPacket(menu.containerId, index, identifier)));
        }
        if (menu instanceof AbstractPlayerInventoryScreenHandler<?>) {
            PlayerUtils.sendNetworkPacket(this.self, new OldPlayerInventoryScreenSyncS2CPacket(menu.containerId));
        }
    }

    @WrapOperation(method = "readAdditionalSaveData", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;readPlayerMode(Lnet/minecraft/world/level/storage/ValueInput;Ljava/lang/String;)Lnet/minecraft/world/level/GameType;", ordinal = 0))
    private GameType setNbtGameMode(ValueInput playerInput, String modeTag, Operation<GameType> original) {
        GameType result = original.call(playerInput, modeTag);
        PlayerComponentCoordinator coordinator = PlayerComponentCoordinator.of(this.self);
        coordinator.setNbtGameMode(result);
        return result;
    }
}
