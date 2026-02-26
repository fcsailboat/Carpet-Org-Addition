package boat.carpetorgaddition.config;

import com.google.gson.JsonPrimitive;
import org.jspecify.annotations.Nullable;

public class BooleanConfig extends AbstractConfig<JsonPrimitive> {
    private final String key;
    private boolean value;

    public BooleanConfig(String key) {
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
    public JsonPrimitive getJsonValue() {
        return new JsonPrimitive(this.value);
    }

    @Override
    protected Class<JsonPrimitive> getType() {
        return JsonPrimitive.class;
    }

    public boolean getBooleanValue() {
        return this.value;
    }
}
