package boat.carpetorgaddition.config;

import com.google.gson.JsonElement;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public abstract class AbstractConfig<T extends JsonElement> implements Comparable<AbstractConfig<T>> {
    public AbstractConfig() {
    }

    public abstract void load(@Nullable T json);

    /**
     * @return 配置的json名称
     */
    public abstract String getKey();

    /**
     * @return 配置的json元素
     */
    public abstract T getJsonValue();

    public boolean shouldBeSaved() {
        return true;
    }

    protected abstract Class<T> getType();

    private int getPriority() {
        return GlobalConfigs.getElementPriority(this.getType());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() == obj.getClass()) {
            AbstractConfig<?> that = (AbstractConfig<?>) obj;
            return Objects.equals(this.getKey(), that.getKey()) && Objects.equals(this.getJsonValue(), that.getJsonValue());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getKey(), this.getJsonValue());
    }

    @Override
    public int compareTo(@NonNull AbstractConfig<T> o) {
        if (this.getType() == o.getType()) {
            return String.CASE_INSENSITIVE_ORDER.compare(this.getKey(), o.getKey());
        }
        return Integer.compare(this.getPriority(), o.getPriority());
    }
}
