package boat.carpetorgaddition.dataupdate.json;

import com.google.gson.JsonObject;

public abstract class DataUpdater {
    public static final String DATA_VERSION = "data_version";
    public static final int ZERO = 0;
    public static final DataUpdater UNCHANGED = new UnchangedDataUpdater();

    protected abstract JsonObject update(JsonObject json, int version);

    public final JsonObject update(JsonObject json, int currentVersion, int targetVersion) {
        JsonObject newJson = this.update(json, currentVersion);
        int actualVersion = DataUpdater.getVersion(newJson);
        if (actualVersion != targetVersion) {
            throw new IllegalStateException("Json update failed: expected target version %d but got version %d (started from version %d)".formatted(targetVersion, actualVersion, currentVersion));
        }
        return newJson;
    }

    public static int getVersion(JsonObject json) {
        if (json.has(DATA_VERSION)) {
            return json.get(DATA_VERSION).getAsInt();
        }
        if (json.has("DataVersion")) {
            return json.get("DataVersion").getAsInt();
        }
        return 0;
    }

    private static class UnchangedDataUpdater extends DataUpdater {
        private UnchangedDataUpdater() {
        }

        @Override
        protected JsonObject update(JsonObject json, int version) {
            return json;
        }
    }
}
