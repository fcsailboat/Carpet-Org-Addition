package boat.carpetorgaddition.logger;

import boat.carpetorgaddition.CarpetOrgAdditionConstants;
import boat.carpetorgaddition.network.s2c.FakePlayerPathfinderS2CPacket;
import boat.carpetorgaddition.util.PlayerUtils;
import carpet.logging.LoggerRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Loggers {
    private static final Map<String, LoggerAccessor> LOGGERS = new HashMap<>();
    /**
     * 流浪商人生成记录器
     *
     * @apiNote wanderingTrader这个名字已经被另一个Carpet扩展使用了
     */
    public static final LoggerAccessor WANDERING_TRADER = register(
            LoggerBuilder.of("wanderingTraderSpawnCountdown")
                    .setType(LoggerType.HUD)
                    .build()
    );
    /**
     * 钓鱼倒计时指示
     */
    public static final LoggerAccessor FISHING = register(
            LoggerBuilder.of("fishing")
                    .setType(LoggerType.FUNCTION)
                    .build()
    );
    /**
     * 黑曜石生成
     */
    public static final LoggerAccessor OBSIDIAN = register(
            LoggerBuilder.of("obsidian")
                    .setType(LoggerType.FUNCTION)
                    .build()
    );
    /**
     * 假玩家寻路
     */
    public static final LoggerAccessor PATHFINDING = register(
            LoggerBuilder.of("fakePlayerPathfinding")
                    .setType(LoggerType.FUNCTION)
                    .setHidden(true)
                    .setUnsubscribeCallback(player -> PlayerUtils.sendNetworkPacket(player, FakePlayerPathfinderS2CPacket.of()))
                    .build()
    );

    /**
     * 注册记录器
     */
    public static void register() {
        for (Map.Entry<String, LoggerAccessor> entry : LOGGERS.entrySet()) {
            LoggerAccessor accessor = entry.getValue();
            if (!accessor.isHidden() || CarpetOrgAdditionConstants.isEnableHiddenFunction()) {
                LoggerRegistry.registerLogger(accessor.getName(), accessor.getLogger());
            }
        }
    }

    private static LoggerAccessor register(LoggerAccessor accessor) {
        LOGGERS.put(accessor.getName(), accessor);
        return accessor;
    }

    public static Optional<LoggerAccessor> getLogger(String name) {
        return Optional.ofNullable(LOGGERS.get(name));
    }
}
