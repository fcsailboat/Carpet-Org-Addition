package boat.carpetorgaddition.wheel.text;

import boat.carpetorgaddition.CarpetOrgAdditionConstants;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;
import java.util.StringJoiner;

@NullMarked
public final class LocalizationKey {
    private static final String ROOT = CarpetOrgAdditionConstants.MOD_ID;
    private final String key;

    private LocalizationKey(String key) {
        this.key = key;
    }

    public static LocalizationKey of(String key) {
        return new LocalizationKey(ROOT + "." + key);
    }

    public static LocalizationKey literal(String key) {
        return new LocalizationKey(key);
    }

    @Deprecated
    public LocalizationKey then(String key, String... keys) {
        StringJoiner joiner = new StringJoiner(".");
        joiner.add(this.key);
        joiner.add(key);
        for (String str : keys) {
            joiner.add(str);
        }
        return new LocalizationKey(joiner.toString());
    }

    public LocalizationKey then(String key) {
        return new LocalizationKey(this.key + "." + key);
    }

    public Component translate(Object... args) {
        String value = Translation.getTranslateValue(this.key);
        return Component.translatableWithFallback(this.key, value, args);
    }

    public TextBuilder builder(Object... args) {
        return TextBuilder.of(this.translate(args));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LocalizationKey that = (LocalizationKey) o;
        return Objects.equals(this.key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.key);
    }

    @Override
    public String toString() {
        return this.key;
    }
}
