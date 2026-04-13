package boat.carpetorgaddition.config;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.util.IOUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CustomCommandConfigEntry implements ConfigEntry<JsonObject> {
    public static final String CUSTOM_COMMAND_NAME = "custom_command_name";
    // 并发集合：客户端命令也会添加到这里
    private final Map<String, Set<String>> commands = new ConcurrentHashMap<>();

    public CustomCommandConfigEntry() {
    }

    @Override
    public String getKey() {
        return CUSTOM_COMMAND_NAME;
    }

    @Override
    public JsonObject getValue() {
        JsonObject json = new JsonObject();
        for (Map.Entry<String, Set<String>> entry : this.commands.entrySet()) {
            String key = entry.getKey();
            Set<String> value = entry.getValue();
            if (value.isEmpty()) {
                // 如果值为空，将键同时做为键和值
                json.addProperty(key, key);
            } else if (value.size() == 1) {
                // 如果值只有一个元素，则值作为字符串
                json.addProperty(key, value.iterator().next());
            } else {
                // 如果值有多个元素，则封装为Json数组
                JsonArray array = new JsonArray();
                for (String str : value) {
                    array.add(str);
                }
                json.add(key, array);
            }
        }
        return json;
    }

    /**
     * 初始化命令配置文件
     */
    private JsonObject migrate(File file) {
        try {
            JsonObject json = IOUtils.loadJson(file).getAsJsonObject("commands");
            IOUtils.deprecatedFile(file);
            return json;
        } catch (Exception e) {
            CarpetOrgAddition.LOGGER.error("Encountered an unexpected error while migrating the custom command names configuration file", e);
        }
        return new JsonObject();
    }

    /**
     * 加载配置文件
     */
    @Override
    public void load(@Nullable JsonObject json) {
        if (json == null) {
            File file = IOUtils.configFile("custom_command_name.json");
            if (file.isFile()) {
                json = this.migrate(file);
            }
        }
        if (json == null) {
            return;
        }
        try {
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                String key = entry.getKey();
                JsonElement value = entry.getValue();
                Set<String> set = switch (value) {
                    case JsonPrimitive primitive when primitive.isString() -> Set.of(value.getAsString());
                    case JsonArray array -> array.asList().stream()
                            .filter(JsonElement::isJsonPrimitive)
                            .map(JsonElement::getAsString)
                            .collect(Collectors.toSet());
                    case null, default -> Set.of(key);
                };
                this.commands.put(key, set);
            }
        } catch (RuntimeException e) {
            // 译：自定义命令名称配置文件已损坏。
            CarpetOrgAddition.LOGGER.warn("The custom command name configuration file is damaged.", e);
        }
    }

    /**
     * 获取命令的自定义名称
     */
    public String[] getCommand(String command) {
        return this.commands.computeIfAbsent(command, Set::of).toArray(String[]::new);
    }
}
