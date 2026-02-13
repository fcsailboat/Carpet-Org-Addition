package boat.carpetorgaddition;

import boat.carpetorgaddition.config.GlobalConfigs;
import boat.carpetorgaddition.debug.DebugRuleRegistrar;
import boat.carpetorgaddition.network.NetworkS2CPacketRegister;
import boat.carpetorgaddition.util.IOUtils;
import boat.carpetorgaddition.wheel.Counter;
import carpet.CarpetServer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
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
import java.util.Locale;
import java.util.StringJoiner;

public class CarpetOrgAddition implements ModInitializer {
    /**
     * 模组ID
     */
    public static final String MOD_ID = "carpet-org-addition";
    /**
     * 模组元数据
     */
    public static final ModMetadata METADATA = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow().getMetadata();
    /**
     * 模组名称
     */
    public static final String MOD_NAME = METADATA.getName();
    /**
     * 不带空格的模组名称
     */
    public static final String COMPACT_MOD_NAME = MOD_NAME.replace(" ", "");
    /**
     * 模组当前的版本
     */
    public static final String VERSION = METADATA.getVersion().getFriendlyString();
    /**
     * 模组名称小写
     */
    public static final String MOD_NAME_LOWER_CASE = COMPACT_MOD_NAME.toLowerCase(Locale.ROOT);
    /**
     * 模组的构建时间戳
     */
    public static final String BUILD_TIMESTAMP = METADATA.getCustomValue("buildTimestamp").getAsString();
    /**
     * 当前jvm是否为调试模式
     */
    public static final boolean IS_DEBUG = ManagementFactory.getRuntimeMXBean().getInputArguments().stream().anyMatch(s -> s.contains("jdwp"));
    /**
     * 当前游戏环境是否为开发环境
     */
    public static final boolean IS_DEVELOPMENT = FabricLoader.getInstance().isDevelopmentEnvironment();
    /**
     * 是否同时加载了{@code Lithium}（锂）模组
     */
    public static final boolean LITHIUM = FabricLoader.getInstance().isModLoaded("lithium");
    /**
     * 是否同时加载了{@code Carpet TIS Addition}模组
     */
    public static final boolean CARPET_TIS_ADDITION = FabricLoader.getInstance().isModLoaded("carpet-tis-addition");
    /**
     * 日志
     */
    public static final Logger LOGGER = LoggerFactory.getLogger(COMPACT_MOD_NAME);
    /**
     * 是否启用隐藏功能<br>
     * <p>
     * <b>请勿</b>传播解锁这些功能的方式。
     * </p>
     */
    public static final boolean ENABLE_HIDDEN_FUNCTION = GlobalConfigs.getInstance().isEnableHiddenFunction();

    /**
     * 模组初始化
     */
    @Override
    public void onInitialize() {
        CarpetServer.manageExtension(new CarpetOrgAdditionExtension());
        // 注册网络数据包
        NetworkS2CPacketRegister.register();
        if (CarpetOrgAddition.ENABLE_HIDDEN_FUNCTION) {
            CarpetOrgAddition.LOGGER.info("Hidden feature enabled");
        }
        if (IS_DEVELOPMENT) {
            this.runs();
        }
        // 如果当前为调试模式的开发环境，注册测试规则
        if (isDebugDevelopment()) {
            DebugRuleRegistrar.getInstance().registrar();
            CarpetOrgAddition.LOGGER.info("Build timestamp: {}", BUILD_TIMESTAMP);
        }
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
            Counter<LocalDate> counter = new Counter<>();
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
            counter.add(now);
            // 获取今天的启动次数
            int count = counter.getCount(now);
            CarpetOrgAddition.LOGGER.info("The game has been launched {} times today", count);
            // 保存启动次数
            List<LocalDate> list = counter.keySet().stream().sorted().toList();
            StringJoiner joiner = new StringJoiner("\n");
            for (LocalDate date : list) {
                joiner.add(date.format(formatter) + "=" + counter.getCount(date));
            }
            IOUtils.write(file, joiner.toString());
            String earliest = list.getFirst().format(formatter);
            CarpetOrgAddition.LOGGER.info("The game has been launched a total of {} times since {}", total, earliest);
        } catch (IOException e) {
            CarpetOrgAddition.LOGGER.warn("An unexpected error occurred while recording the number of game launches", e);
        }
    }

    /**
     * 空方法
     *
     * @apiNote 用于在方法引用中使用
     */
    public static void pass(Object... ignored) {
    }

    /**
     * @return 当前环境是否为调试模式的开发环境
     */
    public static boolean isDebugDevelopment() {
        return IS_DEBUG && IS_DEVELOPMENT;
    }
}
