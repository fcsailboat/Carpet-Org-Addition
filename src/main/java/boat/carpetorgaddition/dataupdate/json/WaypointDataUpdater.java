package boat.carpetorgaddition.dataupdate.json;

import boat.carpetorgaddition.util.IOUtils;
import com.google.gson.JsonObject;

public final class WaypointDataUpdater extends DataUpdater {
    private static final WaypointDataUpdater INSTANCE = new WaypointDataUpdater();

    private WaypointDataUpdater() {
    }

    public static WaypointDataUpdater getInstance() {
        return INSTANCE;
    }

    @Override
    protected JsonObject update(JsonObject oldJson, int version) {
        if (version == 0) {
            int x = IOUtils.getJsonElement(oldJson, "x", Integer.class).orElseThrow();
            int y = IOUtils.getJsonElement(oldJson, "y", Integer.class).orElseThrow();
            int z = IOUtils.getJsonElement(oldJson, "z", Integer.class).orElseThrow();
            String dimension = oldJson.get("dimension").getAsString();
            String creator = oldJson.get("creator").getAsString();
            String illustrate = IOUtils.getJsonElement(oldJson, "illustrate", "", String.class);
            JsonObject anotherPos = new JsonObject();
            if (IOUtils.jsonHasElement(oldJson, "another_x", "another_y", "another_z")) {
                int anotherX = oldJson.get("another_x").getAsInt();
                int anotherY = oldJson.get("another_y").getAsInt();
                int anotherZ = oldJson.get("another_z").getAsInt();
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
            return oldJson;
        }
    }
}
