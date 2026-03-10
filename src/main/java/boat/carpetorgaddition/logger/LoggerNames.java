package boat.carpetorgaddition.logger;

import carpet.logging.Logger;
import carpet.logging.LoggerRegistry;

public class LoggerNames {
    /**
     * 流浪商人生成记录器
     */
    public static final String WANDERING_TRADER_SPAWN_COUNTDOWN = "wanderingTraderSpawnCountdown";
    /**
     * 钓鱼指示记录器
     */
    public static final String FISHING = "fishing";
    /**
     * 黑曜石生成记录器
     */
    public static final String OBSIDIAN = "obsidian";
    public static final String FAKE_PLAYER_PATHFINDING = "fakePlayerPathfinding";

    /**
     * @return 指定名称的记录器
     */
    @Deprecated(forRemoval = true)
    public static Logger getLogger(String loggerName) {
        return LoggerRegistry.getLogger(loggerName);
    }
}
