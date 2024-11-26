package org.carpetorgaddition.logger;

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
     * 信标范围记录器
     */
    public static final String BEACON_RANGE = "beaconRange";
    /**
     * 村民兴趣点记录器
     */
    public static final String VILLAGER = "villager";

    /**
     * @return 指定名称的记录器
     */
    public static Logger getLogger(String loggerName) {
        return LoggerRegistry.getLogger(loggerName);
    }
}
