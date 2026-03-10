package boat.carpetorgaddition.rule;

import boat.carpetorgaddition.dataupdate.json.DataUpdater;
import boat.carpetorgaddition.util.IOUtils;
import boat.carpetorgaddition.util.PlayerUtils;
import boat.carpetorgaddition.wheel.WorldFormat;
import carpet.api.settings.CarpetRule;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CustomRuleValueManager {
    public static final HashMap<String, CarpetRule<?>> NAME_TO_RULES = new HashMap<>();
    private static final BiMap<CarpetRule<?>, CustomRuleControl<?>> RULE_TO_CONTROL = HashBiMap.create();
    private static final int CURRENT_VERSION = 3;
    private final HashMap<String, HashSet<String>> rules = new HashMap<>();
    /**
     * 数据保存的位置
     */
    private final File file;
    /**
     * 规则值是否修改过，如果未修改过，则不重新保存
     */
    private boolean changed = false;

    public CustomRuleValueManager(MinecraftServer server) {
        WorldFormat worldFormat = new WorldFormat(server, null);
        this.file = worldFormat.file("ruleself", "json");
    }

    /**
     * @return 指定规则是否已启用
     */
    public boolean isEnabled(ServerPlayer player, String rule) {
        if (this.rules.isEmpty()) {
            return false;
        }
        HashSet<String> rules = this.rules.get(PlayerUtils.getName(player));
        if (rules == null) {
            return false;
        }
        return rules.contains(rule);
    }

    public boolean isEnabled(ServerPlayer player, CustomRuleControl<?> control) {
        Optional<CustomRuleEntry> optional = get(control);
        return optional.filter(entry -> this.isEnabled(player, entry.getName())).isPresent();
    }

    /**
     * 设置规则是否对自己生效
     */
    public void setEnabled(ServerPlayer player, String rule, boolean enabled) {
        String playerName = PlayerUtils.getName(player);
        HashSet<String> rules = this.rules.get(playerName);
        if (rules == null) {
            if (enabled) {
                HashSet<String> value = new HashSet<>();
                value.add(rule);
                this.rules.put(playerName, value);
                this.changed = true;
            }
            return;
        }
        boolean changed = enabled ? rules.add(rule) : rules.remove(rule);
        if (changed) {
            this.changed = true;
        }
        // 玩家已经关闭了所有仅对自己生效的规则，从集合中删除
        if (rules.isEmpty()) {
            this.rules.remove(playerName);
        }
    }

    public void onServerSave() {
        this.save();
    }

    /**
     * 将功能开关保存为如下的json格式<br>
     * <ul>
     *     <li>根</li>
     * <ul>
     *     <li>{@code data_version}：数据版本</li>
     *     <li>{@code ruleself}：一个json对象，记录每个玩家的功能开关</li>
     *     <ul>
     *         <li>一名玩家，可能不存在</li>
     *           <ul>
     *              <li>一个json数组，记录每一条被禁用的规则，可能不存在</li>
     *          </ul>
     *     </ul>
     * </ul>
     * </ul>
     * 示例：<br>
     * <blockquote>
     *     <pre>
     * {
     *   "data_version": 1,
     *   "ruleself": {
     *     "Steve": [
     *       "blockDropsDirectlyEnterInventory"
     *     ]
     *   }
     * }
     *     </pre>
     * </blockquote>
     */
    public void save() {
        if (this.changed) {
            try {
                JsonObject json = new JsonObject();
                // 添加数据版本属性
                json.addProperty(DataUpdater.DATA_VERSION, CURRENT_VERSION);
                JsonObject ruleSelfJson = this.createRuleSelfJson();
                json.add("ruleself", ruleSelfJson);
                IOUtils.write(this.file, json);
            } catch (IOException e) {
                IOUtils.loggerError(e);
            }
        }
    }

    private JsonObject createRuleSelfJson() {
        JsonObject json = new JsonObject();
        // json的键为玩家名，值为一个字符串数组
        for (Map.Entry<String, HashSet<String>> entry : this.rules.entrySet()) {
            JsonArray array = new JsonArray();
            HashSet<String> value = entry.getValue();
            for (String rule : value) {
                array.add(rule);
            }
            json.add(entry.getKey(), array);
        }
        return json;
    }

    public void load() {
        if (this.file.isFile()) {
            try {
                JsonObject json = IOUtils.loadJson(this.file);
                JsonObject ruleself = json.get("ruleself").getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : ruleself.entrySet()) {
                    HashSet<String> rules = new HashSet<>();
                    for (JsonElement element : entry.getValue().getAsJsonArray()) {
                        rules.add(element.getAsString());
                    }
                    this.rules.put(entry.getKey(), rules);
                }
            } catch (IOException e) {
                IOUtils.loggerError(e);
            }
        }
    }

    public static void put(CustomRuleControl<?> control, CarpetRule<?> rule) {
        NAME_TO_RULES.put(rule.name(), rule);
        RULE_TO_CONTROL.put(rule, control);
    }

    public static Optional<CustomRuleEntry> get(String name) {
        CarpetRule<?> rule = NAME_TO_RULES.get(name);
        CustomRuleControl<?> control = RULE_TO_CONTROL.get(rule);
        return CustomRuleEntry.of(name, rule, control);
    }

    public static Optional<CustomRuleEntry> get(@NotNull CarpetRule<?> rule) {
        CustomRuleControl<?> control = RULE_TO_CONTROL.get(rule);
        String name = rule.name();
        return CustomRuleEntry.of(name, rule, control);
    }

    public static Optional<CustomRuleEntry> get(CustomRuleControl<?> control) {
        CarpetRule<?> rule = RULE_TO_CONTROL.inverse().get(control);
        if (rule == null) {
            return Optional.empty();
        }
        String name = rule.name();
        return CustomRuleEntry.of(name, rule, control);
    }

    public static List<String> values() {
        return NAME_TO_RULES.values().stream().map(CarpetRule::name).toList();
    }
}
