package org.carpetorgaddition.util.wheel;

import carpet.CarpetServer;
import carpet.api.settings.SettingsManager;
import com.google.gson.JsonObject;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.util.IOUtils;

import java.io.File;
import java.io.IOException;

@SuppressWarnings("unused")
public class ConfigManager {
    public static final String CONFIG_PATH = "./config/%s".formatted(CarpetOrgAddition.MOD_NAME_LOWER_CASE);
    private static final File SETTINGS_MANAGER_CONFIG = new File(CONFIG_PATH, "settings_manager.json");

    public static void initSettingsManagerConfigs() {
        if (SETTINGS_MANAGER_CONFIG.isFile()) {
            return;
        }
        if (IOUtils.createFileIfNotExists(SETTINGS_MANAGER_CONFIG)) {
            JsonObject json = new JsonObject();
            json.addProperty("custom_settings_manager", false);
            json.addProperty("identifier", CarpetOrgAddition.MOD_ID);
            json.addProperty("fancy_name", CarpetOrgAddition.MOD_NAME);
            try {
                IOUtils.saveJson(SETTINGS_MANAGER_CONFIG, json);
            } catch (IOException e) {
                CarpetOrgAddition.LOGGER.error("初始化{}配置时出现意外错误", CarpetOrgAddition.MOD_NAME, e);
            }
        }
    }

    /**
     * 此功能暂不可用，原因：Carpet会根据规则管理器的标识符来获取翻译
     */
    public static SettingsManager getSettingManager() {
        JsonObject json;
        try {
            json = IOUtils.loadJson(SETTINGS_MANAGER_CONFIG);
        } catch (IOException e) {
            CarpetOrgAddition.LOGGER.error("无法正确读取配置文件{}，正在使用默认规则管理器", SETTINGS_MANAGER_CONFIG.getName(), e);
            return CarpetServer.settingsManager;
        }
        try {
            if (json.get("custom_settings_manager").getAsBoolean()) {
                String identifier = json.get("identifier").getAsString();
                if ("carpet".equals(identifier)) {
                    return CarpetServer.settingsManager;
                }
                String fancyName = json.get("fancy_name").getAsString();
                return new SettingsManager(CarpetOrgAddition.VERSION, identifier, fancyName);
            }
        } catch (RuntimeException e) {
            CarpetOrgAddition.LOGGER.error("无法正确解析json文件，正在使用默认规则管理器", e);
        }
        return CarpetServer.settingsManager;
    }
}
