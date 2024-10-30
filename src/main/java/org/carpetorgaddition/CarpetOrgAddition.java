package org.carpetorgaddition;

import carpet.CarpetServer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.carpetorgaddition.debug.DebugRuleRegistrar;
import org.carpetorgaddition.debug.DebugSettings;
import org.carpetorgaddition.network.NetworkS2CPackRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;

public class CarpetOrgAddition implements ModInitializer {
    /**
     * 控制玩家登录登出的消息是否显示
     */
    public static boolean hiddenLoginMessages = false;
    /**
     * 日志
     */
    public static final Logger LOGGER = LoggerFactory.getLogger("CarpetOrgAddition");
    /**
     * 模组名称小写
     */
    public static final String MOD_NAME_LOWER_CASE = "carpetorgaddition";
    /**
     * 模组ID
     */
    public static final String MOD_ID = "carpet-org-addition";
    /**
     * 当前jvm是否为调试模式
     */
    public static final boolean IS_DEBUG = ManagementFactory.getRuntimeMXBean().getInputArguments().stream().anyMatch(s -> s.contains("jdwp"));

    /**
     * 模组初始化
     */
    @Override
    public void onInitialize() {
        CarpetServer.manageExtension(new CarpetOrgAdditionExtension());
        NetworkS2CPackRegister.register();
        // 如果当前为调试模式的开发环境，注册测试规则
        if (isDebugDevelopment()) {
            DebugRuleRegistrar.getInstance().registrar(DebugSettings.class);
        }
    }

    /**
     * @return 当前环境是否为调试模式的开发环境
     */
    public static boolean isDebugDevelopment() {
        return IS_DEBUG && FabricLoader.getInstance().isDevelopmentEnvironment();
    }
    // TODO 发布前测试在非开发环境能否正常启动
}
