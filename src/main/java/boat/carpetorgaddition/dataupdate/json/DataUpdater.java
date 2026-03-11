package boat.carpetorgaddition.dataupdate.json;

import com.google.gson.JsonObject;

public abstract class DataUpdater {
    public static final String DATA_VERSION = "data_version";
    @Deprecated(forRemoval = true)
    public static final int VERSION = 3;
    public static final int ZERO = 0;
    public static final DataUpdater UNCHANGED = new UnchangedDataUpdater();

    protected abstract JsonObject update(JsonObject oldJson, int version);

    public final JsonObject update(JsonObject oldJson, int currentVersion, int targetVersion) {
        JsonObject newJson = this.update(oldJson, currentVersion);
        if (DataUpdater.getVersion(newJson) != targetVersion) {
            throw new IllegalStateException("Json has not been updated to the target version");
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
        protected JsonObject update(JsonObject oldJson, int version) {
            return oldJson;
        }
    }
}
