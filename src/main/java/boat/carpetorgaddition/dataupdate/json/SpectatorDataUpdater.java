package boat.carpetorgaddition.dataupdate.json;

import com.google.gson.JsonObject;

public final class SpectatorDataUpdater implements DataUpdater {
    private static final SpectatorDataUpdater INSTANCE = new SpectatorDataUpdater();

    private SpectatorDataUpdater() {
    }

    public static SpectatorDataUpdater getInstance() {
        return INSTANCE;
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    @Override
    public JsonObject update(JsonObject oldJson, int version) {
        return switch (version) {
            case 0 -> {
                double x = oldJson.get("x").getAsDouble();
                double y = oldJson.get("y").getAsDouble();
                double z = oldJson.get("z").getAsDouble();
                double yaw = oldJson.get("yaw").getAsDouble();
                double pitch = oldJson.get("pitch").getAsDouble();
                String dimension = oldJson.get("dimension").getAsString();
                JsonObject newJson = new JsonObject();
                newJson.addProperty("data_version", 1);
                JsonObject pos = new JsonObject();
                pos.addProperty("x", x);
                pos.addProperty("y", y);
                pos.addProperty("z", z);
                newJson.add("pos", pos);
                JsonObject direction = new JsonObject();
                direction.addProperty("yaw", yaw);
                direction.addProperty("pitch", pitch);
                newJson.add("direction", direction);
                newJson.addProperty("dimension", dimension);
                yield this.update(newJson, 1);
            }
            default -> oldJson;
        };
    }
}
