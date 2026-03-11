package boat.carpetorgaddition.rule;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.CarpetOrgAdditionExtension;
import boat.carpetorgaddition.dataupdate.json.CarpetConfDataUpdater;
import boat.carpetorgaddition.dataupdate.json.DataUpdater;
import boat.carpetorgaddition.util.IOUtils;
import boat.carpetorgaddition.wheel.WorldFormat;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.InvalidRuleValueException;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RuleConfig {
    public static final String CONFIG_FINE_NAME = "config.json";
    public static final String RULES = "rules";
    private final MinecraftServer server;
    private final File file;
    private static final int CURRENT_VERSION = 4;

    public RuleConfig(MinecraftServer server) {
        this.server = server;
        this.file = new WorldFormat(server, null).file(CONFIG_FINE_NAME);
    }

    /**
     * 读取配置文件
     */
    public void load() {
        CommandSourceStack source = this.server.createCommandSourceStack();
        for (Map.Entry<String, String> entry : this.read().entrySet()) {
            CarpetRule<?> rule = CarpetOrgAdditionExtension.getSettingManager().getCarpetRule(entry.getKey());
            if (rule == null) {
                CarpetOrgAddition.LOGGER.warn("Unknown rule: {} - ignoring...", entry.getKey());
                continue;
            }
            try {
                rule.set(source, entry.getValue());
            } catch (InvalidRuleValueException e) {
                CarpetOrgAddition.LOGGER.warn("Couldn't set value for rule {}", entry.getKey(), e);
            }
        }
    }

    public void put(CarpetRule<?> rule, String value) {
        Map<String, String> map = this.read();
        map.put(rule.name(), value);
        this.save(map);
    }

    public void remove(CarpetRule<?> rule) {
        Map<String, String> map = this.read();
        if (map.remove(rule.name()) == null) {
            return;
        }
        this.save(map);
    }

    private Map<String, String> read() {
        HashMap<String, String> map = new HashMap<>();
        if (this.file.isFile()) {
            try {
                JsonObject json = IOUtils.loadJson(this.file);
                CarpetConfDataUpdater updater = CarpetConfDataUpdater.getInstance();
                json = updater.update(json, DataUpdater.getVersion(json), CURRENT_VERSION);
                for (Map.Entry<String, JsonElement> entry : json.get(RULES).getAsJsonObject().entrySet()) {
                    map.put(entry.getKey(), entry.getValue().getAsString());
                }
            } catch (IOException | RuntimeException e) {
                CarpetOrgAddition.LOGGER.warn("When reading Carpet Org Addition rules, an unexpected error occurred", e);
            }
        }
        return map;
    }

    private void save(Map<String, String> map) {
        JsonObject json = new JsonObject();
        json.addProperty(DataUpdater.DATA_VERSION, CURRENT_VERSION);
        JsonObject rules = new JsonObject();
        map.forEach(rules::addProperty);
        json.add(RULES, rules);
        save(json);
    }

    private void save(JsonObject json) {
        try {
            IOUtils.write(this.file, json);
        } catch (IOException e) {
            CarpetOrgAddition.LOGGER.warn("When saving Carpet Org Addition rules, an unexpected error occurred", e);
        }
    }

    public void migrate(JsonObject rules) {
        JsonObject json = new JsonObject();
        json.addProperty(DataUpdater.DATA_VERSION, DataUpdater.ZERO);
        json.add(RULES, rules);
        CarpetConfDataUpdater dataUpdater = CarpetConfDataUpdater.getInstance();
        JsonObject update = dataUpdater.update(json, DataUpdater.getVersion(json), CURRENT_VERSION);
        this.save(update);
        CarpetOrgAddition.LOGGER.info("The Carpet Org Addition rules have been migrated from carpet.conf to carpetorgaddition/config.json");
    }

    /**
     * 配置文件数据是否已经迁移
     */
    public boolean isMigrated() {
        return this.file.isFile();
    }
}
