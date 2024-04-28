package org.carpet_org_addition.mixin.logger;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.WanderingTraderManager;
import org.carpet_org_addition.logger.WanderingTraderSpawnLogger;
import org.carpet_org_addition.logger.WanderingTraderSpawnLogger.SpawnCountdown;
import org.carpet_org_addition.util.MessageUtils;
import org.carpet_org_addition.util.TextUtils;
import org.carpet_org_addition.util.WorldUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(WanderingTraderManager.class)
public class WanderingTraderManagerMixin {
    @Shadow
    private int spawnDelay;

    @Shadow
    private int spawnTimer;

    @Shadow
    private int spawnChance;

    @Inject(method = "spawn", at = @At("HEAD"))
    private void updataLogger(ServerWorld world, boolean spawnMonsters, boolean spawnAnimals, CallbackInfoReturnable<Integer> cir) {
        if (world.getGameRules().getBoolean(GameRules.DO_TRADER_SPAWNING)) {
            // 获取流浪商人生成的倒计时，并换算成秒
            int countdown = ((this.spawnDelay == 0 ? 1200 : this.spawnDelay) - (1200 - this.spawnTimer)) / 20;
            WanderingTraderSpawnLogger.setSpawnCountdown(new SpawnCountdown(countdown, this.spawnChance));
            return;
        }
        WanderingTraderSpawnLogger.setSpawnCountdown(null);
    }

    @WrapOperation(method = "trySpawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/WanderingTraderEntity;setPositionTarget(Lnet/minecraft/util/math/BlockPos;I)V"))
    private void broadcastSpawnSuccess(WanderingTraderEntity trader, BlockPos blockPos, int i, Operation<Void> original) {
        original.call(trader, blockPos, i);
        if (WanderingTraderSpawnLogger.wanderingTrader && WanderingTraderSpawnLogger.spawnCountdownNonNull()) {
            // 获取流浪商人所在的服务器
            MinecraftServer server = trader.getWorld().getServer();
            if (server == null) {
                return;
            }
            PlayerManager playerManager = server.getPlayerManager();
            // 广播流浪商人生成成功
            MessageUtils.broadcastTextMessage(playerManager,
                    TextUtils.getTranslate("carpet.logger.wanderingTrader.message",
                            TextUtils.blockPos(trader.getBlockPos(), Formatting.GREEN)));
            List<ServerPlayerEntity> list = playerManager.getPlayerList();
            // 播放音效通知流浪商人生成
            for (ServerPlayerEntity player : list) {
                WorldUtils.playSound(trader.getWorld(), player.getBlockPos(), trader.getYesSound(), trader.getSoundCategory());
            }
        }
    }
}
