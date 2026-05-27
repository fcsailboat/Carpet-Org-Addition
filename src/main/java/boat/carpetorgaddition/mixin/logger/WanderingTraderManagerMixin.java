package boat.carpetorgaddition.mixin.logger;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.logger.LoggerAccessor;
import boat.carpetorgaddition.logger.Loggers;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.misc.WanderingTraderSpawnData;
import boat.carpetorgaddition.wheel.misc.WanderingTraderSpawnData.SpawnCountdown;
import boat.carpetorgaddition.wheel.provider.CommandProvider;
import boat.carpetorgaddition.wheel.provider.TextProvider;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTraderSpawner;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.saveddata.WanderingTraderData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Set;

@Mixin(WanderingTraderSpawner.class)
public abstract class WanderingTraderManagerMixin {
    @Shadow
    protected abstract WanderingTraderData getTraderData();

    @Shadow
    private int tickDelay;

    @Inject(method = "tick", at = @At("HEAD"))
    private void updateLogger(ServerLevel level, boolean spawnEnemies, CallbackInfo ci) {
        if (level.getGameRules().get(GameRules.SPAWN_WANDERING_TRADERS)) {
            WanderingTraderData data = this.getTraderData();
            // 获取流浪商人生成的倒计时，并换算成秒
            int countdown = ((data.spawnDelay() == 0 ? 1200 : data.spawnDelay()) - (1200 - this.tickDelay)) / 20;
            WanderingTraderSpawnData.setSpawnCountdown(new SpawnCountdown(countdown, data.spawnChance()));
            return;
        }
        WanderingTraderSpawnData.setSpawnCountdown(null);
    }

    @WrapOperation(method = "spawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/wanderingtrader/WanderingTrader;setHomeTo(Lnet/minecraft/core/BlockPos;I)V"))
    private void broadcastSpawnSuccess(WanderingTrader trader, BlockPos pos, int i, Operation<Void> original) {
        original.call(trader, pos, i);
        LoggerAccessor accessor = Loggers.WANDERING_TRADER;
        if (accessor.isEnable() && WanderingTraderSpawnData.spawnCountdownNonNull()) {
            // 获取流浪商人所在的服务器
            MinecraftServer server = ServerUtils.getServer(trader);
            if (server == null) {
                return;
            }
            Set<Map.Entry<String, String>> entries = accessor.getSubscribedOnlinePlayers().entrySet();
            // 普通消息
            Component blockPos = TextProvider.blockPos(trader.blockPosition(), ChatFormatting.GREEN);
            for (Map.Entry<String, String> entry : entries) {
                ServerPlayer player = server.getPlayerList().getPlayerByName(entry.getKey());
                if (player == null) {
                    continue;
                }
                // 广播流浪商人生成成功
                if (CarpetOrgAdditionSettings.COMMAND_NAVIGATE.value().hasPermission(player)) {
                    // 带点击导航的消息
                    Component button = TextBuilder.of(LocalizationKeys.Button.NAVIGATE.translate())
                            .setCommand(CommandProvider.navigateToUuidEntity(trader.getUUID()))
                            .setHover(LocalizationKeys.Button.NAVIGATE_HOVER.translate(trader.getName()))
                            .setColor(ChatFormatting.AQUA)
                            .build();
                    MessageUtils.sendMessage(player, accessor.getLocalizationKey().then("message_with_navigate").translate(blockPos, button));
                } else {
                    Component message = accessor.getLocalizationKey().then("message").translate(blockPos);
                    MessageUtils.sendMessage(player, message);
                }
                // 播放音效通知流浪商人生成
                ServerUtils.playSound(ServerUtils.getWorld(trader), player.blockPosition(), trader.getNotifyTradeSound(), trader.getSoundSource());
            }
        }
    }
}
