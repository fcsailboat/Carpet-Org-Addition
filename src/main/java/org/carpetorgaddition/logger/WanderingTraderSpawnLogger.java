package org.carpetorgaddition.logger;

import net.minecraft.server.MinecraftServer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.world.GameRules;
import org.carpetorgaddition.util.TextUtils;

/**
 * 流浪商人生成记录器
 */
public class WanderingTraderSpawnLogger {
    private static SpawnCountdown spawnCountdown;

    // 更新HUD
    public static void updateHud(MinecraftServer server) {
        if (server.getGameRules().getBoolean(GameRules.DO_TRADER_SPAWNING)) {
            if (LoggerRegister.wanderingTrader && spawnCountdown != null) {
                // 计算流浪商人生成概率的百分比
                double chance = spawnCountdown.spawnChance / 10.0;
                MutableText time;
                // 流浪商人生成倒计时（单位：秒）
                int spawnCountdown = WanderingTraderSpawnLogger.spawnCountdown.countdown + 1;
                if (spawnCountdown <= 60) {
                    // 小于60秒
                    time = TextUtils.translate("carpet.logger.wanderingTrader.time.second", spawnCountdown);
                } else if (spawnCountdown % 60 == 0) {
                    // 整分
                    time = TextUtils.translate("carpet.logger.wanderingTrader.time.minutes", spawnCountdown / 60);
                } else {
                    // %s分%s秒
                    time = TextUtils.translate("carpet.logger.wanderingTrader.time.minutes_and_seconds",
                            spawnCountdown / 60, spawnCountdown % 60);
                }
                Loggers.getWanderingTraderLogger().log((s, playerEntity) -> new Text[]{
                        TextUtils.translate("carpet.logger.wanderingTrader.hud", time, (String.format("%.1f", chance) + "%"))
                });
            }
        } else {
            Loggers.getWanderingTraderLogger().log((s, playerEntity)
                    -> new Text[]{TextUtils.translate("carpet.logger.wanderingTrader.gamerule.not_enabled",
                    TextUtils.translate(GameRules.DO_TRADER_SPAWNING.getTranslationKey()))});
        }
    }

    // 当前生成倒计时是否为null
    public static boolean spawnCountdownNonNull() {
        return spawnCountdown != null;
    }

    public static void setSpawnCountdown(SpawnCountdown spawnCountdown) {
        WanderingTraderSpawnLogger.spawnCountdown = spawnCountdown;
    }

    public static class SpawnCountdown {
        // 距离下一次流浪商人生成剩下的时间
        private final int countdown;
        // 流浪商人生成的概率
        private final int spawnChance;

        public SpawnCountdown(int countdown, int spawnChance) {
            this.countdown = countdown;
            this.spawnChance = spawnChance;
        }
    }
}
