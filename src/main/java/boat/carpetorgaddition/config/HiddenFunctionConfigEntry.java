package boat.carpetorgaddition.config;

import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.Nullable;

public class HiddenFunctionConfigEntry implements ConfigEntry<JsonPrimitive> {
    private boolean enable;
    private boolean shouldBeSaved = false;

    public HiddenFunctionConfigEntry() {
    }

    @Override
    public void load(@Nullable JsonPrimitive json) {
        if (json == null) {
            this.enable = false;
        } else {
            this.shouldBeSaved = true;
            this.enable = json.getAsBoolean();
        }
    }

    @Override
    public String getKey() {
        return "enableHiddenFunction";
    }

    @Override
    public boolean shouldBeSaved() {
        return this.shouldBeSaved;
    }

    @Override
    public JsonPrimitive getValue() {
        return new JsonPrimitive(this.enable);
    }

    public boolean isEnable() {
        return this.enable;
    }
}
