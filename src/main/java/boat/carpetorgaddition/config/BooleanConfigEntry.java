package boat.carpetorgaddition.config;

import com.google.gson.JsonPrimitive;
import org.jspecify.annotations.Nullable;

public class BooleanConfigEntry implements ConfigEntry<JsonPrimitive> {
    private final String key;
    private boolean value;

    public BooleanConfigEntry(String key) {
        this.key = key;
    }

    @Override
    public void load(@Nullable JsonPrimitive json) {
        this.value = json != null && json.getAsBoolean();
    }

    @Override
    public String getKey() {
        return this.key;
    }

    @Override
    public JsonPrimitive getValue() {
        return new JsonPrimitive(this.value);
    }

    @Override
    public Class<JsonPrimitive> getType() {
        return JsonPrimitive.class;
    }

    public boolean getBooleanValue() {
        return this.value;
    }
}
