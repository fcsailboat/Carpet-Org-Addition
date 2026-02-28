package boat.carpetorgaddition;

import boat.carpetorgaddition.debug.DebugRuleRegistrar;
import boat.carpetorgaddition.network.NetworkS2CPacketRegister;
import boat.carpetorgaddition.util.IOUtils;
import boat.carpetorgaddition.wheel.SimpleCounter;
import carpet.CarpetServer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.StringJoiner;

public class CarpetOrgAddition implements ModInitializer {
    /**
     * 日志
     */
    public static final Logger LOGGER = LoggerFactory.getLogger(CarpetOrgAdditionMetadata.COMPACT_MOD_NAME);
    /**
     * 当前游戏环境是否为开发环境
     */
    private static final boolean IS_DEVELOPMENT = FabricLoader.getInstance().isDevelopmentEnvironment();
    /**
     * 当前jvm是否为调试模式
     */
    private static final boolean IS_DEBUG = IS_DEVELOPMENT && ManagementFactory.getRuntimeMXBean().getInputArguments().stream().anyMatch(s -> s.contains("jdwp"));

    /**
     * @return 当前环境是否为调试模式的开发环境
     */
    public static boolean isDebugMode() {
        return IS_DEBUG;
    }

    public static boolean isDevelopment() {
        return IS_DEVELOPMENT;
    }

    /**
     * 空方法
     *
     * @apiNote 用于在方法引用中使用
     */
    public static void pass(Object... ignored) {
    }

    /**
     * 模组初始化
     */
    @Override
    public void onInitialize() {
        if (IS_DEBUG) {
            CarpetOrgAddition.LOGGER.debug("The game is starting in debug mode");
            // 如果当前为调试模式的开发环境，注册测试规则
            DebugRuleRegistrar.getInstance().registrar();
        }
        CarpetServer.manageExtension(CarpetOrgAdditionExtension.getInstance());
        // 注册网络数据包
        NetworkS2CPacketRegister.register();
        if (CarpetOrgAdditionConstants.isEnableHiddenFunction()) {
            CarpetOrgAddition.LOGGER.debug("Hidden feature enabled");
        }
        if (IS_DEVELOPMENT) {
            this.runs();
        }
        CarpetOrgAddition.LOGGER.debug("Build timestamp: {}", CarpetOrgAdditionMetadata.BUILD_TIMESTAMP);
    }

    /**
     * 记录游戏的启动次数
     */
    private void runs() {
        File file = Path.of("startlog.txt").toAbsolutePath().toFile();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        try {
            // 自有记录以来的启动次数
            int total = 0;
            SimpleCounter<LocalDate> counter = new SimpleCounter<>();
            if (file.isFile()) {
                // 读取历史启动次数
                List<String> list = Files.readAllLines(file.toPath());
                for (String str : list) {
                    if (str.isEmpty()) {
                        continue;
                    }
                    String[] split = str.split("=");
                    if (split.length != 2) {
                        continue;
                    }
                    LocalDate localDate = LocalDate.parse(split[0], formatter);
                    int count = Integer.parseInt(split[1]);
                    counter.add(localDate, count);
                    total += count;
                }
            }
            LocalDate now = LocalDate.now();
            total++;
            counter.increment(now);
            // 获取今天的启动次数
            int count = counter.getCount(now);
            CarpetOrgAddition.LOGGER.debug("The game has been launched {} times today", count);
            // 保存启动次数
            List<LocalDate> list = counter.keySet().stream().sorted().toList();
            StringJoiner joiner = new StringJoiner("\n");
            for (LocalDate date : list) {
                joiner.add(date.format(formatter) + "=" + counter.getCount(date));
            }
            IOUtils.write(file, joiner.toString());
            String earliest = list.getFirst().format(formatter);
            CarpetOrgAddition.LOGGER.debug("The game has been launched a total of {} times since {}", total, earliest);
        } catch (IOException e) {
            CarpetOrgAddition.LOGGER.warn("An unexpected error occurred while recording the number of game launches", e);
        }
    }
}
