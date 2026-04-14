package boat.carpetorgaddition.rule;

import boat.carpetorgaddition.CarpetOrgAdditionExtension;
import boat.carpetorgaddition.exception.TranslatableInvalidRuleValueException;
import boat.carpetorgaddition.rule.validator.StrictValueValidator;
import boat.carpetorgaddition.rule.validator.ValueValidator;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.LocalizationKeys.Data.Type;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.RuleHelper;
import carpet.api.settings.SettingsManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

public class BuiltRule<T> implements CarpetRule<T> {
    /**
     * 规则值是否改变了，用于隐藏规则更改后的命令反馈
     */
    public static final ThreadLocal<Boolean> RULE_UNCHANGED = ThreadLocal.withInitial(() -> false);
    private final String name;
    private final String displayName;
    private final String displayDesc;
    private final Collection<String> categories;
    private final Collection<String> suggestions;
    @NonNull
    private T value;
    private final T defaultValue;
    private final boolean canBeToggledClientSide;
    private final Class<T> type;
    private final RuleValueParser<T> parser;
    /**
     * 规则更改验证器
     */
    private final List<ValueValidator<T>> valueValidators = new ArrayList<>();
    /**
     * 规则更改静默验证器
     */
    private final List<SilenceValueValidator<T>> silenceValidators = new ArrayList<>();
    /**
     * 规则更改监听器
     */
    private final List<RuleListener<T>> listeners = new ArrayList<>();
    private final boolean strict;
    /**
     * 规则是否被启用了
     */
    private boolean enable = false;

    public BuiltRule(
            Class<T> type,
            String name,
            Collection<String> categories,
            Collection<String> suggestions,
            @NonNull T value,
            boolean canBeToggledClientSide,
            List<ValueValidator<T>> valueValidators,
            List<SilenceValueValidator<T>> silenceValidators,
            List<RuleListener<T>> listeners,
            boolean strict,
            String displayName,
            String displayDesc
    ) {
        this.name = name;
        this.categories = categories;
        this.suggestions = suggestions;
        this.value = value;
        this.defaultValue = value;
        this.canBeToggledClientSide = canBeToggledClientSide;
        this.type = type;
        this.valueValidators.addAll(valueValidators);
        this.silenceValidators.addAll(silenceValidators);
        this.listeners.addAll(listeners);
        this.strict = strict;
        this.parser = createParser();
        this.displayName = displayName;
        this.displayDesc = displayDesc;
        this.init();
    }

    private void init() {
        if (this.strict()) {
            this.valueValidators.addFirst(new StrictValueValidator<>(this));
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private RuleValueParser<T> createParser() {
        Map.Entry<Component, Function<String, T>> entry = switch (this.defaultValue) {
            case String _ -> Map.entry(Type.STRING.translate(), s -> this.type.cast(s.toLowerCase(Locale.ROOT)));
            case Boolean _ -> Map.entry(Type.BOOLEAN.translate(), s -> this.type.cast(parseBoolean(s)));
            case Integer _ -> Map.entry(Type.INTEGER.translate(), s -> this.type.cast(Integer.parseInt(s)));
            case Long _ -> Map.entry(Type.LONG.translate(), s -> this.type.cast(Long.parseLong(s)));
            case Double _ -> Map.entry(Type.DOUBLE.translate(), s -> this.type.cast(Double.parseDouble(s)));
            case Float _ -> Map.entry(Type.FLOAT.translate(), s -> this.type.cast(Float.parseFloat(s)));
            case Enum<?> _ -> Map.entry(Type.ENUM.translate(), s -> {
                Class<? extends Enum> clazz = (Class<? extends Enum>) this.type;
                // 只有枚举名称全部为大写时才能匹配
                return (T) Enum.valueOf(clazz, s.toUpperCase(Locale.ROOT));
            });
            default -> {
                String message = "Unsupported type for %s %s".formatted(this.getClass().getSimpleName(), this.type);
                throw new UnsupportedOperationException(message);
            }
        };
        return str -> {
            Component dataType = entry.getKey();
            try {
                return entry.getValue().apply(str);
            } catch (RuntimeException e) {
                TextBuilder builder = LocalizationKeys.Data.UNABLE_TO_PARSE.builder(Type.STRING.translate(), str, dataType);
                builder.setHover(e);
                throw new TranslatableInvalidRuleValueException(builder.build());
            }
        };
    }

    /**
     * 将字符串解析为布尔值
     *
     * @apiNote jdk默认的解析布尔值不会抛出异常
     */
    private Boolean parseBoolean(String str) {
        return switch (str.toLowerCase(Locale.ROOT)) {
            case "true" -> Boolean.TRUE;
            case "false" -> Boolean.FALSE;
            default -> throw new IllegalArgumentException("Invalid boolean value: %s".formatted(str));
        };
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public List<Component> extraInfo() {
        return RuleUtils.ruleExtraInfo(this);
    }

    @Override
    public Collection<String> categories() {
        return this.categories;
    }

    @Override
    public Collection<String> suggestions() {
        return this.suggestions;
    }

    @Override
    public SettingsManager settingsManager() {
        return CarpetOrgAdditionExtension.getSettingManager();
    }

    @Override
    public T value() {
        return this.value;
    }

    @Override
    public boolean canBeToggledClientSide() {
        return this.canBeToggledClientSide;
    }

    @Override
    public Class<T> type() {
        return this.type;
    }

    @Override
    public T defaultValue() {
        return this.defaultValue;
    }

    @Override
    public boolean strict() {
        return this.strict && !this.suggestions().isEmpty();
    }

    @Override
    public void set(@Nullable CommandSourceStack source, String value) throws TranslatableInvalidRuleValueException {
        this.set(source, this.parser.pause(value), value);
    }

    @Override
    public void set(@Nullable CommandSourceStack source, T value) throws TranslatableInvalidRuleValueException {
        this.set(source, value, RuleHelper.toRuleString(value));
    }

    private void set(@Nullable CommandSourceStack source, T value, String userInput) throws TranslatableInvalidRuleValueException {
        for (ValueValidator<T> valueValidator : this.valueValidators) {
            if (valueValidator.validate(value)) {
                continue;
            }
            if (source != null) {
                valueValidator.notifyFailure(source, this, userInput);
            }
            throw new TranslatableInvalidRuleValueException();
        }
        if (value.equals(this.value()) && source != null) {
            return;
        }
        boolean canChanged = this.silenceValidators.stream().allMatch(observer -> observer.validate(source, value));
        if (canChanged) {
            this.value = value;
            this.enable = value instanceof Boolean ? (Boolean) value : !this.value.equals(this.defaultValue);
            if (source != null) {
                this.settingsManager().notifyRuleChanged(source, this, userInput);
            }
            this.listeners.forEach(listener -> listener.onChanged(source, value));
        } else {
            RULE_UNCHANGED.set(true);
        }
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public String getDisplayDesc() {
        return this.displayDesc;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BuiltRule<?> that = (BuiltRule<?>) o;
        return Objects.equals(this.name, that.name);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public String toString() {
        return this.name + ": " + RuleHelper.toRuleString(value());
    }

    public boolean isEnable() {
        return this.enable;
    }

    @FunctionalInterface
    private interface RuleValueParser<T> {
        T pause(String str) throws TranslatableInvalidRuleValueException;
    }
}
