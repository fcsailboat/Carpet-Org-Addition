package boat.carpetorgaddition.config;

import com.google.gson.JsonPrimitive;
import org.jspecify.annotations.Nullable;

public class IntegerConfigEntry implements ConfigEntry<JsonPrimitive> {
    private final String key;
    private final int defaultValue;
    private int value;

    public IntegerConfigEntry(String key, int defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    @Override
    public void load(@Nullable JsonPrimitive json) {
        this.value = json == null ? this.defaultValue : json.getAsInt();
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

    public int getIntValue() {
        return this.value;
    }
}
