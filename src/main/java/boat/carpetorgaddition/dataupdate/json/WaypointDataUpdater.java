package boat.carpetorgaddition.dataupdate.json;

import com.google.gson.JsonObject;

public final class WaypointDataUpdater extends DataUpdater {
    private static final WaypointDataUpdater INSTANCE = new WaypointDataUpdater();

    private WaypointDataUpdater() {
    }

    public static WaypointDataUpdater getInstance() {
        return INSTANCE;
    }

    @Override
    protected JsonObject update(JsonObject json, int version) {
        if (version == 0) {
            int x = json.get("x").getAsInt();
            int y = json.get("y").getAsInt();
            int z = json.get("z").getAsInt();
            String dimension = json.get("dimension").getAsString();
            String creator = json.get("creator").getAsString();
            String illustrate = json.has("illustrate") ? json.get("illustrate").getAsString() : "";
            JsonObject anotherPos = new JsonObject();
            if (json.has("another_x") && json.has("another_y") && json.has("another_z")) {
                int anotherX = json.get("another_x").getAsInt();
                int anotherY = json.get("another_y").getAsInt();
                int anotherZ = json.get("another_z").getAsInt();
                anotherPos.addProperty("x", anotherX);
                anotherPos.addProperty("y", anotherY);
                anotherPos.addProperty("z", anotherZ);
            }
            JsonObject newJson = new JsonObject();
            JsonObject pos = new JsonObject();
            pos.addProperty("x", x);
            pos.addProperty("y", y);
            pos.addProperty("z", z);
            newJson.addProperty("data_version", 3);
            newJson.add("pos", pos);
            newJson.addProperty("dimension", dimension);
            newJson.addProperty("creator", creator);
            newJson.addProperty("comment", illustrate);
            newJson.add("another_pos", anotherPos);
            return this.update(newJson, 3);
        } else {
            return json;
        }
    }
}
