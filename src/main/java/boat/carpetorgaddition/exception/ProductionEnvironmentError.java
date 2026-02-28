package boat.carpetorgaddition.exception;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.CarpetOrgAdditionMetadata;

/**
 * 生成环境错误
 *
 * @apiNote 继承了 {@link Error} 而不是{@link Exception}，因为这在非开发环境不应该出现
 */
public class ProductionEnvironmentError extends AssertionError {
    private ProductionEnvironmentError() {
    }

    /**
     * 断言当前环境为开发环境
     */
    public static void assertDevelopmentEnvironment() {
        if (CarpetOrgAddition.isDevelopment()) {
            return;
        }
        // 除非发生了逻辑错误，否则永远不会执行到这里
        CarpetOrgAddition.LOGGER.error("{}遇到了严重的逻辑错误：生产环境中执行了测试代码", CarpetOrgAdditionMetadata.MOD_NAME);
        CarpetOrgAddition.LOGGER.error("请附带完整的游戏日志提交问题至：https://github.com/fcsailboat/Carpet-Org-Addition/issues");
        CarpetOrgAddition.LOGGER.error("{} encountered a critical logic error: Test code executed in production environment", CarpetOrgAdditionMetadata.MOD_NAME);
        CarpetOrgAddition.LOGGER.error("Please submit the issue with complete game logs at: https://github.com/fcsailboat/Carpet-Org-Addition/issues");
        throw new ProductionEnvironmentError();
    }
}
