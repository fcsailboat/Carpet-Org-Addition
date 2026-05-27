package boat.carpetorgaddition.config;

import com.google.gson.JsonPrimitive;
import org.jspecify.annotations.Nullable;

public class BooleanConfigEntry implements ConfigEntry<JsonPrimitive> {
    private final String key;
    private final boolean defaultValue;
    private boolean value;

    public BooleanConfigEntry(String key, boolean defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    @Override
    public void load(@Nullable JsonPrimitive json) {
        this.value = json == null ? this.defaultValue : json.getAsBoolean();
    }

    @Override
    public String getKey() {
        return this.key;
    }

    @Override
    public JsonPrimitive getValue() {
        return new JsonPrimitive(this.value);
    }

    public boolean getBooleanValue() {
        return this.value;
    }
}
