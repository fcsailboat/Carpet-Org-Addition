package boat.carpetorgaddition.dataupdate.json;

import boat.carpetorgaddition.periodic.fakeplayer.action.EmptyTheContainerAction;
import boat.carpetorgaddition.periodic.fakeplayer.action.FillTheContainerAction;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.predicate.ItemStackPredicate;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashSet;
import java.util.Map;

public final class FakePlayerActionDataUpdater extends DataUpdater {
    private static final FakePlayerActionDataUpdater INSTANCE = new FakePlayerActionDataUpdater();

    private FakePlayerActionDataUpdater() {
    }

    public static FakePlayerActionDataUpdater getInstance() {
        return INSTANCE;
    }

    @Override
    protected JsonObject update(JsonObject oldJson, int version) {
        if (version == 0) {
            HashSet<Map.Entry<String, JsonObject>> entries = new HashSet<>();
            // 通常只会循环一次，因为一名玩家只能同时有一个动作
            for (Map.Entry<String, JsonElement> oldActionEntry : oldJson.entrySet()) {
                Map.Entry<String, DataUpdater> actionUpdaterEntry = getDataUpdater(oldActionEntry.getKey(), version);
                String key = actionUpdaterEntry.getKey();
                DataUpdater dataUpdater = actionUpdaterEntry.getValue();
                // 更新数据
                JsonObject newJson = dataUpdater.update(oldActionEntry.getValue().getAsJsonObject(), version);
                entries.add(Map.entry(key, newJson));
            }
            JsonObject newJson = new JsonObject();
            for (Map.Entry<String, JsonObject> entry : entries) {
                newJson.add(entry.getKey(), entry.getValue());
            }
            return newJson;
        }
        return oldJson;
    }

    /**
     * @return 左值：键的新名称，右值：数据更新器
     */
    private Map.Entry<String, DataUpdater> getDataUpdater(String key, int version) {
        if (version == 0) {
            return switch (key) {
                case "clean" -> Map.entry("empty_the_container", EmptyTheContainerActionDataUpdater.INSTANCE);
                case "fill" -> Map.entry("fill_the_container", FillTheContainerActionDataUpdater.INSTANCE);
                case "inventory_crafting" -> Map.entry("inventory_craft", DataUpdater.UNCHANGED);
                case "sorting" -> Map.entry("categorize", DataUpdater.UNCHANGED);
                case "planting" -> Map.entry("plant", DataUpdater.UNCHANGED);
                default -> Map.entry(key, DataUpdater.UNCHANGED);
            };
        }
        return Map.entry(key, DataUpdater.UNCHANGED);
    }

    /**
     * 清空容器动作数据更新器
     */
    public static final class EmptyTheContainerActionDataUpdater extends DataUpdater {
        private static final EmptyTheContainerActionDataUpdater INSTANCE = new EmptyTheContainerActionDataUpdater();

        private EmptyTheContainerActionDataUpdater() {
        }

        @Override
        protected JsonObject update(JsonObject oldJson, int version) {
            if (version == 0) {
                if (oldJson.has("allItem") && oldJson.get("allItem").getAsBoolean()) {
                    // 匹配任意物品
                    ItemStackPredicate predicate = ItemStackPredicate.WILDCARD;
                    JsonObject newJson = new JsonObject();
                    newJson.addProperty(EmptyTheContainerAction.ITEM, predicate.toString());
                    return newJson;
                } else {
                    // 匹配指定物品
                    ItemStackPredicate predicate;
                    if (oldJson.has(EmptyTheContainerAction.ITEM)) {
                        String item = oldJson.get(EmptyTheContainerAction.ITEM).getAsString();
                        predicate = new ItemStackPredicate(ServerUtils.asItem(item));
                    } else {
                        predicate = ItemStackPredicate.WILDCARD;
                    }
                    JsonObject newJson = new JsonObject();
                    newJson.addProperty(EmptyTheContainerAction.ITEM, predicate.toString());
                    return newJson;
                }
            }
            return oldJson;
        }
    }

    public static final class FillTheContainerActionDataUpdater extends DataUpdater {
        private static final FillTheContainerActionDataUpdater INSTANCE = new FillTheContainerActionDataUpdater();

        private FillTheContainerActionDataUpdater() {
        }

        @Override
        protected JsonObject update(JsonObject oldJson, int version) {
            if (version == 0) {
                String key = FillTheContainerAction.DROP_OTHER;
                // dropOther默认为true
                boolean dropOther = !oldJson.has(key) || oldJson.get(key).getAsBoolean();
                ItemStackPredicate predicate;
                if (oldJson.has("allItem") && oldJson.get("allItem").getAsBoolean()) {
                    // 匹配任意物品
                    predicate = ItemStackPredicate.WILDCARD;
                } else if (oldJson.has(FillTheContainerAction.ITEM)) {
                    // 匹配指定物品
                    String itemId = oldJson.get(FillTheContainerAction.ITEM).getAsString();
                    predicate = new ItemStackPredicate(ServerUtils.asItem(itemId));
                } else {
                    predicate = ItemStackPredicate.WILDCARD;
                }
                JsonObject newJson = new JsonObject();
                newJson.addProperty(FillTheContainerAction.ITEM, predicate.toString());
                newJson.addProperty(FillTheContainerAction.DROP_OTHER, dropOther);
                return newJson;
            }
            return oldJson;
        }
    }
}
