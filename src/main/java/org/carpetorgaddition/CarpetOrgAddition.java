package org.carpetorgaddition;

import carpet.CarpetServer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import org.carpetorgaddition.debug.DebugRuleRegistrar;
import org.carpetorgaddition.network.s2c.NetworkS2CPacketRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.util.Locale;

public class CarpetOrgAddition implements ModInitializer {
    /**
     * 日志
     */
    public static final Logger LOGGER = LoggerFactory.getLogger("CarpetOrgAddition");
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
     * 模组当前的版本
     */
    public static final String VERSION = METADATA.getVersion().getFriendlyString();
    /**
     * 模组名称小写
     */
    public static final String MOD_NAME_LOWER_CASE = MOD_NAME.replace(" ", "").toLowerCase(Locale.ROOT);
    /**
     * 当前jvm是否为调试模式
     */
    public static final boolean IS_DEBUG = ManagementFactory.getRuntimeMXBean().getInputArguments().stream().anyMatch(s -> s.contains("jdwp"));

    /**
     * 模组初始化
     */
    @Override
    public void onInitialize() {
        LOGGER.info("%s已加载，版本：%s".formatted(MOD_NAME, VERSION));
        CarpetServer.manageExtension(new CarpetOrgAdditionExtension());
        NetworkS2CPacketRegister.register();
        // 如果当前为调试模式的开发环境，注册测试规则
        if (isDebugDevelopment()) {
            DebugRuleRegistrar.getInstance().registrar();
        }
    }

    /**
     * @return 当前环境是否为调试模式的开发环境
     */
    public static boolean isDebugDevelopment() {
        return IS_DEBUG && FabricLoader.getInstance().isDevelopmentEnvironment();
    }
}
