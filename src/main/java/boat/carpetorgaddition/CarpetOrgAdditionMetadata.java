package boat.carpetorgaddition;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;

public class CarpetOrgAdditionMetadata {
    /**
     * 模组ID
     */
    public static final String MOD_ID = "carpet-org-addition";
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
    /**
     * 模组元数据
     */
    public static final ModMetadata METADATA = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow().getMetadata();
    /**
     * 模组的构建时间戳
     */
    public static final String BUILD_TIMESTAMP = METADATA.getCustomValue("build_timestamp").getAsString();
    /**
     * 模组当前的版本
     */
    public static final String VERSION = METADATA.getVersion().getFriendlyString();
}
