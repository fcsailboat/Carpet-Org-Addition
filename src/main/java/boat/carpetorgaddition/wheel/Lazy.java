package boat.carpetorgaddition.wheel;

import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.function.Supplier;

public final class Lazy<T> {
    private final Supplier<T> initializer;
    private T instance;

    private Lazy(Supplier<T> initializer) {
        this.initializer = initializer;
    }

    public static <T> Lazy<T> of(Supplier<@NonNull T> initializer) {
        return new Lazy<>(initializer);
    }

    public T get() {
        if (this.instance == null) {
            this.instance = Objects.requireNonNull(this.initializer.get());
        }
        return this.instance;
    }
}
