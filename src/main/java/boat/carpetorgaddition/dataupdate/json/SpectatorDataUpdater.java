package boat.carpetorgaddition.dataupdate.json;

import com.google.gson.JsonObject;

public final class SpectatorDataUpdater extends DataUpdater {
    private static final SpectatorDataUpdater INSTANCE = new SpectatorDataUpdater();

    private SpectatorDataUpdater() {
    }

    public static SpectatorDataUpdater getInstance() {
        return INSTANCE;
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    @Override
    protected JsonObject update(JsonObject json, int version) {
        return switch (version) {
            case 0 -> {
                double x = json.get("x").getAsDouble();
                double y = json.get("y").getAsDouble();
                double z = json.get("z").getAsDouble();
                double yaw = json.get("yaw").getAsDouble();
                double pitch = json.get("pitch").getAsDouble();
                String dimension = json.get("dimension").getAsString();
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
            default -> json;
        };
    }
}
