package boat.carpetorgaddition.config;

import com.google.gson.JsonElement;
import org.jspecify.annotations.Nullable;

public interface ConfigEntry<T extends JsonElement> {
    void load(@Nullable T json);

    /**
     * @return 配置的json名称
     */
    String getKey();

    /**
     * @return 配置的json元素
     */
    T getValue();

    default boolean shouldBeSaved() {
        return true;
    }
}
