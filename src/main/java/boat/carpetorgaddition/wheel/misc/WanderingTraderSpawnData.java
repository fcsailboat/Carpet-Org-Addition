package boat.carpetorgaddition.wheel.misc;

import boat.carpetorgaddition.logger.LoggerAccessor;
import boat.carpetorgaddition.logger.Loggers;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * 流浪商人生成记录器
 */
public class WanderingTraderSpawnData {
    private static SpawnCountdown spawnCountdown;

    // 更新HUD
    public static void updateHud(MinecraftServer server) {
        LoggerAccessor accessor = Loggers.WANDERING_TRADER;
        if (server.overworld().getGameRules().get(GameRules.SPAWN_WANDERING_TRADERS)) {
            if (accessor.isEnable() && spawnCountdown != null) {
                // 计算流浪商人生成概率的百分比
                double chance = spawnCountdown.spawnChance / 10.0;
                Component time;
                // 流浪商人生成倒计时（单位：秒）
                int spawnCountdown = WanderingTraderSpawnData.spawnCountdown.countdown + 1;
                if (spawnCountdown <= 60) {
                    // 小于60秒
                    time = LocalizationKeys.Time.SECOND.translate(spawnCountdown);
                } else if (spawnCountdown % 60 == 0) {
                    // 整分
                    time = LocalizationKeys.Time.MINUTE.translate(spawnCountdown / 60);
                } else {
                    // %s分%s秒
                    time = LocalizationKeys.Time.MINUTE_SECOND.translate(spawnCountdown / 60, spawnCountdown % 60);
                }
                accessor.getLogger().log((_, _) -> {
                    Component message = accessor.getLocalizationKey().then("hud").translate(time, (String.format("%.1f", chance) + "%"));
                    return new Component[]{message};
                });
            }
        } else {
            accessor.getLogger().log((_, _) -> {
                Component gamerule = LocalizationKey.literal(GameRules.SPAWN_WANDERING_TRADERS.getDescriptionId()).translate();
                return new Component[]{accessor.getLocalizationKey().then("gamerule_not_enabled").translate(gamerule)};
            });
        }
    }

    // 当前生成倒计时是否为null
    public static boolean spawnCountdownNonNull() {
        return spawnCountdown != null;
    }

    public static void setSpawnCountdown(SpawnCountdown spawnCountdown) {
        WanderingTraderSpawnData.spawnCountdown = spawnCountdown;
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
