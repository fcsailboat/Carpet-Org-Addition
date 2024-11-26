package org.carpetorgaddition.exception;

import net.fabricmc.loader.api.FabricLoader;

/**
 * 生成环境错误
 *
 * @apiNote 继承了 {@link Error} 而不是{@link Exception}，因为这在非开发环境不应该出现
 */
public class ProductionEnvironmentError extends Error {
    public ProductionEnvironmentError() {
    }

    /**
     * 断言当前环境为开发环境
     */
    public static void assertDevelopmentEnvironment() {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            return;
        }
        throw new ProductionEnvironmentError();
    }
}
