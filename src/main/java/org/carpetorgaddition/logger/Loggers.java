package org.carpetorgaddition.logger;

import carpet.logging.HUDLogger;
import carpet.logging.LoggerRegistry;

public class Loggers {
    /**
     * 流浪商人生成记录器
     */
    public static HUDLogger getWanderingTraderLogger() {
        return (HUDLogger) LoggerRegistry.getLogger(LoggerNames.WANDERING_TRADER_SPAWN_COUNTDOWN);
    }

    /**
     * 钓鱼记录器
     */
    public static FunctionLogger getFishingLogger() {
        return (FunctionLogger) LoggerRegistry.getLogger(LoggerNames.FISHING);
    }

    /**
     * 信标范围记录器
     */
    public static NetworkPacketLogger getBeaconRangeLogger() {
        return (NetworkPacketLogger) LoggerRegistry.getLogger(LoggerNames.BEACON_RANGE);
    }

    /**
     * 村民兴趣点记录器
     */
    public static NetworkPacketLogger getVillagerLogger() {
        return (NetworkPacketLogger) LoggerRegistry.getLogger(LoggerNames.VILLAGER);
    }

    /**
     * 黑曜石记录器
     */
    public static FunctionLogger getObsidianLogger() {
        return (FunctionLogger) LoggerRegistry.getLogger(LoggerNames.OBSIDIAN);
    }
}
