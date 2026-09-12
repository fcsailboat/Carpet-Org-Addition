package boat.carpetorgaddition.dataupdate.json;

import boat.carpetorgaddition.periodic.fakeplayer.PlayerSerializationManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.Map;

public final class FakePlayerSerializerDataUpdater extends DataUpdater {
    private static final FakePlayerSerializerDataUpdater INSTANCE = new FakePlayerSerializerDataUpdater();

    private FakePlayerSerializerDataUpdater() {
    }

    public static FakePlayerSerializerDataUpdater getInstance() {
        return INSTANCE;
    }

    @Override
    protected JsonObject update(JsonObject oldJson, int version) {
        return switch (version) {
            case 0, 1, 2 -> {
                // 更新玩家动作数据
                if (oldJson.has(PlayerSerializationManager.SCRIPT_ACTION)) {
                    JsonObject scriptJson = oldJson.get(PlayerSerializationManager.SCRIPT_ACTION).getAsJsonObject();
                    FakePlayerActionDataUpdater updater = FakePlayerActionDataUpdater.getInstance();
                    JsonObject newJson = updater.update(scriptJson, version);
                    if (newJson != scriptJson) {
                        oldJson.add(PlayerSerializationManager.SCRIPT_ACTION, newJson);
                    }
                }
                oldJson.addProperty("data_version", 3);
                yield this.update(oldJson, 3);
            }
            case 3 -> {
                JsonObject newJson = new JsonObject();
                for (Map.Entry<String, JsonElement> entry : oldJson.entrySet()) {
                    switch (entry.getKey()) {
                        case "hand_action" -> newJson.add("simple_action", entry.getValue());
                        case "startup" -> {
                            JsonArray oldArray = entry.getValue().getAsJsonArray();
                            JsonArray newArray = new JsonArray();
                            List<JsonObject> list = oldArray.asList().stream()
                                    .map(JsonElement::getAsJsonObject)
                                    .toList();
                            for (JsonObject oldAction : list) {
                                JsonObject newAction = new JsonObject();
                                for (Map.Entry<String, JsonElement> actionPart : oldAction.entrySet()) {
                                    if ("action".equals(actionPart.getKey())) {
                                        JsonObject function = new JsonObject();
                                        function.addProperty("type", "simple");
                                        function.add("value", actionPart.getValue());
                                        newAction.add("function", function);
                                    } else {
                                        newAction.add(actionPart.getKey(), actionPart.getValue());
                                    }
                                }
                                newArray.add(newAction);
                            }
                            newJson.add("startup_action", newArray);
                        }
                        default -> newJson.add(entry.getKey(), entry.getValue());
                    }
                }
                newJson.addProperty("data_version", 4);
                yield this.update(newJson, 4);
            }
            case 4 -> {
                JsonObject newJson = new JsonObject();
                for (Map.Entry<String, JsonElement> entry : oldJson.entrySet()) {
                    JsonElement value = entry.getValue();
                    if ("script_action".equals(entry.getKey())) {
                        JsonObject newAction = new JsonObject();
                        for (Map.Entry<String, JsonElement> actionEntry : value.getAsJsonObject().entrySet()) {
                            if ("rename".equals(actionEntry.getKey())) {
                                JsonObject newRename = new JsonObject();
                                for (Map.Entry<String, JsonElement> renameEntry : actionEntry.getValue().getAsJsonObject().entrySet()) {
                                    String key = renameEntry.getKey();
                                    newRename.add("new_name".equals(key) ? "name" : key, renameEntry.getValue());
                                }
                                newAction.add(actionEntry.getKey(), newRename);
                            } else {
                                newAction.add(actionEntry.getKey(), actionEntry.getValue());
                            }
                        }
                        newJson.add(entry.getKey(), newAction);
                    } else {
                        newJson.add(entry.getKey(), value);
                    }
                }
                newJson.addProperty("data_version", 5);
                yield this.update(newJson, 5);
            }
            default -> oldJson;
        };
    }
}
