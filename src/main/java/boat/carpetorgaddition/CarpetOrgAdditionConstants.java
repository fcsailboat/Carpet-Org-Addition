package boat.carpetorgaddition;

import boat.carpetorgaddition.config.GlobalConfigs;
import carpet.CarpetSettings;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;

import java.nio.file.Path;

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
     * 模组ID
     */
    public static final String MOD_ID = "carpet-org-addition";
    /**
     * 模组元数据
     */
    public static final ModMetadata METADATA = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow().getMetadata();
    /**
     * 模组当前的版本
     */
    public static final String VERSION = METADATA.getVersion().getFriendlyString();
    /**
     * 模组的构建时间戳
     */
    public static final String BUILD_TIMESTAMP = METADATA.getCustomValue("build_timestamp").getAsString();
    /**
     * 模组名称
     */
    public static final String MOD_NAME = "Carpet Org Addition";
    /**
     * 不带空格的模组名称
     */
    public static final String COMPACT_MOD_NAME = "CarpetOrgAddition";
    /**
     * 模组名称小写
     */
    public static final String COMPACT_MOD_NAME_LOWER_CASE = "carpetorgaddition";
    private static String language = null;
    private static Path configDirectory = null;
    private static Path gameDirectory = null;

    /**
     * 是否启用隐藏功能<br>
     * <p>
     * <b>请勿</b>传播解锁这些功能的方式。
     * </p>
     */
    public static boolean isEnableHiddenFunction() {
        return HiddenFunctionHolder.ENABLE_HIDDEN_FUNCTION;
    }

    public static String getCarpetLanguage() {
        return language == null ? CarpetSettings.language : language;
    }

    public static Path getConfigDirectory() {
        if (configDirectory == null) {
            configDirectory = FabricLoader.getInstance().getConfigDir();
        }
        return configDirectory;
    }

    public static Path getGameDirectory() {
        if (gameDirectory == null) {
            gameDirectory = FabricLoader.getInstance().getGameDir();
        }
        return gameDirectory;
    }

    public static void setCarpetLanguage(String lang) {
        if (CarpetOrgAdditionConstants.language == null) {
            CarpetOrgAdditionConstants.language = lang;
        } else {
            throw new UnsupportedOperationException("The carpet language can only be set once");
        }
    }

    public static void setConfigDirectory(Path path) {
        if (CarpetOrgAdditionConstants.configDirectory == null) {
            CarpetOrgAdditionConstants.configDirectory = path;
        } else {
            throw new UnsupportedOperationException("The config file directory can only be set once");
        }
    }

    public static void setGameDirectory(Path path) {
        if (CarpetOrgAdditionConstants.gameDirectory == null) {
            CarpetOrgAdditionConstants.gameDirectory = path;
        } else {
            throw new UnsupportedOperationException("The game directory can only be set once");
        }
    }

    /**
     * @apiNote 用于推迟字段初始化
     */
    private static class HiddenFunctionHolder {
        private static final boolean ENABLE_HIDDEN_FUNCTION = GlobalConfigs.getInstance().isEnableHiddenFunction();
    }
}
