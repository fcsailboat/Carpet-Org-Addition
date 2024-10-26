package org.carpetorgaddition.mixin.logger;

import carpet.logging.Logger;
import carpet.utils.CommandHelper;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.WanderingTraderManager;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.logger.WanderingTraderSpawnLogger;
import org.carpetorgaddition.logger.WanderingTraderSpawnLogger.SpawnCountdown;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.WorldUtils;
import org.carpetorgaddition.util.constant.TextConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Set;

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
        if (WanderingTraderSpawnLogger.wanderingTraderSpawnCountdown && WanderingTraderSpawnLogger.spawnCountdownNonNull()) {
            // 获取流浪商人所在的服务器
            MinecraftServer server = trader.getWorld().getServer();
            if (server == null) {
                return;
            }
            Logger logger = WanderingTraderSpawnLogger.getLogger();
            Set<Map.Entry<String, String>> entries = ((LoggerAccessor) logger).getSubscribedOnlinePlayers().entrySet();
            // 普通消息
            MutableText message = TextUtils.translate("carpet.logger.wanderingTrader.message",
                    TextConstants.blockPos(trader.getBlockPos(), Formatting.GREEN));
            // 带点击导航的消息
            MutableText command = TextUtils.command(TextUtils.translate("carpet.logger.wanderingTrader.message.navigate"),
                    "/navigate uuid \"" + trader.getUuid().toString() + "\"",
                    TextUtils.translate("carpet.logger.wanderingTrader.message.navigate.hover", trader.getName()),
                    Formatting.AQUA, false);
            MutableText canClickMessage = TextUtils.translate("carpet.logger.wanderingTrader.message.click",
                    TextConstants.blockPos(trader.getBlockPos(), Formatting.GREEN), command);
            for (Map.Entry<String, String> entry : entries) {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
                if (player == null) {
                    continue;
                }
                // 广播流浪商人生成成功
                MessageUtils.sendMessage(player, CommandHelper.canUseCommand(player.getCommandSource(),
                        CarpetOrgAdditionSettings.commandNavigate) ? canClickMessage : message);
                // 播放音效通知流浪商人生成
                WorldUtils.playSound(trader.getWorld(), player.getBlockPos(), trader.getYesSound(), trader.getSoundCategory());
            }
        }
    }
}
