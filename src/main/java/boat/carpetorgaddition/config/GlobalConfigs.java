package boat.carpetorgaddition.config;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.CarpetOrgAdditionConstants;
import boat.carpetorgaddition.dataupdate.json.DataUpdater;
import boat.carpetorgaddition.util.IOUtils;
import boat.carpetorgaddition.util.MathUtils;
import com.google.gson.*;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GlobalConfigs {
    /**
     * 配置文件的路径
     */
    private static final File CONFIG_FILE = IOUtils.CONFIGURE_DIRECTORY.resolve(CarpetOrgAdditionConstants.MOD_ID + ".json").toFile();
    private static final List<ConfigEntry<?>> ALL_CONFIGS = new ArrayList<>();
    private static final int CURRENT_VERSION = 3;
    /**
     * 自定义命令名称
     */
    private static final CustomCommandConfigEntry CUSTOM_COMMAND_CONFIG = register(new CustomCommandConfigEntry());
    /**
     * 启用隐藏功能
     */
    private static final HiddenFunctionConfigEntry HIDDEN_FUNCTION_CONFIG = register(new HiddenFunctionConfigEntry());
    /**
     * 是否允许在多人游戏中使用{@code /playerManager}命令来设置玩家登录时执行命令
     */
    private static final BooleanConfigEntry ALLOW_MP_PLAYER_STARTUP_CMD = register(new BooleanConfigEntry("allow_mp_player_startup_cmd", false));
    /**
     * {@code /playerAction}命令中，物品分拣支持的最大物品数量
     */
    private static final IntegerConfigEntry PLAYER_ACTION_MAX_SORTING_ITEMS = register(new IntegerConfigEntry("player_action_max_sorting_items", 16));
    private static final GlobalConfigState INSTANCE = new GlobalConfigState(CONFIG_FILE, ALL_CONFIGS);

    private GlobalConfigs() {
    }

    public static void save() {
        INSTANCE.save();
    }

    private static <C extends ConfigEntry<T>, T extends JsonElement> C register(C config) {
        ALL_CONFIGS.add(config);
        return config;
    }

    /**
     * @return 是否启用了隐藏功能
     */
    public static boolean isEnableHiddenFunction() {
        return HIDDEN_FUNCTION_CONFIG.isEnable();
    }

    public static String[] getCommand(String command) {
        return CUSTOM_COMMAND_CONFIG.getCommand(command);
    }

    public static boolean isAllowMpPlayerStartupCmd() {
        return ALLOW_MP_PLAYER_STARTUP_CMD.getBooleanValue();
    }

    public static int getPlayerActionMaxSortingItemCount() {
        int value = PLAYER_ACTION_MAX_SORTING_ITEMS.getIntValue();
        return MathUtils.clamp(value, 1, 256);
    }

    public static final class GlobalConfigState {
        /**
         * 配置文件的路径
         */
        private final File configFile;
        private final List<ConfigEntry<?>> configs;
        @NonNull
        private JsonObject cachedJson;

        private GlobalConfigState(File file, List<ConfigEntry<?>> configs) {
            this.configFile = file;
            this.configs = Collections.unmodifiableList(configs);
            this.init();
        }

        private void init() {
            if (this.configFile.isFile()) {
                JsonObject json;
                try {
                    json = IOUtils.loadJson(this.configFile);
                } catch (IOException e) {
                    json = this.initJsonObject();
                }
                this.cachedJson = json;
            } else {
                this.cachedJson = this.initJsonObject();
            }
            this.register();
        }

        private JsonObject initJsonObject() {
            CarpetOrgAddition.LOGGER.info("Initializing configuration file");
            JsonObject json = new JsonObject();
            json.addProperty(DataUpdater.DATA_VERSION, CURRENT_VERSION);
            return json;
        }

        private void register() {
            for (ConfigEntry<?> config : this.configs) {
                this.load(config);
            }
        }

        @SuppressWarnings("unchecked")
        private <E extends JsonElement, C extends ConfigEntry<E>> void load(C config) {
            JsonElement element = this.cachedJson.get(config.getKey());
            try {
                config.load((E) element);
            } catch (RuntimeException e) {
                IOUtils.backupFile(this.configFile);
                this.cachedJson.add(config.getKey(), config.getValue());
                CarpetOrgAddition.LOGGER.warn("Global config partially corrupted - resetting damaged section", e);
            }
        }

        private void save() {
            try {
                List<Map.Entry<String, JsonElement>> list = new ArrayList<>(this.cachedJson.entrySet());
                this.configs.stream()
                        .filter(ConfigEntry::shouldBeSaved)
                        .forEach(config -> list.add(Map.entry(config.getKey(), config.getValue())));
                JsonObject json = new JsonObject();
                list.stream().sorted(this.comparator()).forEach(entry -> json.add(entry.getKey(), entry.getValue()));
                IOUtils.write(this.configFile, json);
                this.cachedJson = json;
            } catch (IOException e) {
                CarpetOrgAddition.LOGGER.error("An unexpected error occurred while saving the global configuration file for {}", CarpetOrgAdditionConstants.MOD_NAME, e);
            }
        }

        private Comparator<Map.Entry<String, JsonElement>> comparator() {
            return (o1, o2) -> {
                Class<? extends JsonElement> o1Class = o1.getValue().getClass();
                Class<? extends JsonElement> o2Class = o2.getValue().getClass();
                if (o1Class == o2Class) {
                    String o1Key = o1.getKey();
                    String o2Key = o2.getKey();
                    if (DataUpdater.DATA_VERSION.equals(o1Key) && !DataUpdater.DATA_VERSION.equals(o2Key)) {
                        return -1;
                    }
                    if (!DataUpdater.DATA_VERSION.equals(o1Key) && DataUpdater.DATA_VERSION.equals(o2Key)) {
                        return 1;
                    }
                    return String.CASE_INSENSITIVE_ORDER.compare(o1Key, o2Key);
                }
                return Integer.compare(getElementPriority(o1Class), getElementPriority(o2Class));
            };
        }

        private <T extends JsonElement> int getElementPriority(Class<T> type) {
            if (type == JsonPrimitive.class || type == JsonNull.class) {
                return 1;
            }
            if (type == JsonObject.class || type == JsonArray.class) {
                return 2;
            }
            return 3;
        }
    }
}
