package org.carpetorgaddition.logger;

import carpet.logging.HUDLogger;
import carpet.logging.Logger;
import carpet.logging.LoggerRegistry;

import java.lang.reflect.Field;

public class Loggers {
    // wanderingTrader这个名字已经被另一个Carpet扩展使用了
    @LoggerConfig(name = LoggerNames.WANDERING_TRADER_SPAWN_COUNTDOWN, type = LoggerType.HUD)
    public static boolean wanderingTrader = false;

    @LoggerConfig(name = LoggerNames.FISHING, type = LoggerType.STANDARD)
    public static boolean fishing = false;

    /**
     * 注册记录器
     */
    public static void register() {
        for (Field field : Loggers.class.getFields()) {
            LoggerConfig annotation = field.getAnnotation(LoggerConfig.class);
            if (annotation == null) {
                continue;
            }
            // 记录器名称
            String name = annotation.name();
            Logger logger = createLogger(field, annotation, name);
            LoggerRegistry.registerLogger(name, logger);
        }
    }

    /**
     * 创建一个新记录器
     */
    private static Logger createLogger(Field field, LoggerConfig annotation, String name) {
        // 记录器默认选项
        String defaultOption = annotation.defaultOption().isEmpty() ? null : annotation.defaultOption();
        // 记录器选项
        String[] options = annotation.options();
        // 记录器选项是否严格
        boolean strictOptions = annotation.strictOptions();
        return switch (annotation.type()) {
            case STANDARD -> new Logger(field, name, defaultOption, options, strictOptions);
            case HUD -> new HUDLogger(field, name, defaultOption, options, strictOptions);
        };
    }
}
