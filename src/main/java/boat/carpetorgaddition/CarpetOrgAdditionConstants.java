package boat.carpetorgaddition;

import boat.carpetorgaddition.config.GlobalConfigs;
import net.fabricmc.loader.api.FabricLoader;

public class CarpetOrgAdditionConstants {
    /**
     * 是否同时加载了{@code Lithium}（锂）模组
     */
    public static final boolean LITHIUM = FabricLoader.getInstance().isModLoaded("lithium");
    /**
     * 是否同时加载了{@code Carpet TIS Addition}模组
     */
    public static final boolean CARPET_TIS_ADDITION = FabricLoader.getInstance().isModLoaded("carpet-tis-addition");
    /**
     * 模组{@code gugle-carpet-addition}是否已加载
     */
    public static final boolean GCA = FabricLoader.getInstance().isModLoaded("gca");

    /**
     * 是否启用隐藏功能<br>
     * <p>
     * <b>请勿</b>传播解锁这些功能的方式。
     * </p>
     */
    public static boolean isEnableHiddenFunction() {
        return HiddenFunctionHolder.ENABLE_HIDDEN_FUNCTION;
    }

    /**
     * @apiNote 用于推迟字段初始化
     */
    private static class HiddenFunctionHolder {
        private static final boolean ENABLE_HIDDEN_FUNCTION = GlobalConfigs.getInstance().isEnableHiddenFunction();
    }
}
