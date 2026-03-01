package boat.carpetorgaddition.rule;

import boat.carpetorgaddition.CarpetOrgAdditionConstants;
import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.rule.validator.ValueValidator;
import boat.carpetorgaddition.util.ServerUtils;
import carpet.api.settings.CarpetRule;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class RuleFactory {
    public static <T> Builder<T> create(Class<T> type, String rule, T value) {
        return new Builder<>(type, rule, value);
    }

    public static class Builder<T> {
        private final Class<T> type;
        private final String name;
        private final Collection<String> categories = new ArrayList<>();
        private final Collection<String> suggestions;
        @NotNull
        private final T value;
        private final List<ValueValidator<T>> validators = new ArrayList<>();
        private final List<SilenceValueValidator<T>> silenceValidators = new ArrayList<>();
        private final List<RuleListener<T>> listeners = new ArrayList<>();
        private final List<Supplier<Boolean>> conditions = new ArrayList<>();
        private boolean canBeToggledClientSide = false;
        private boolean strict = true;
        private boolean isHidden;
        private boolean isRemove;
        private CustomRuleControl<T> control;
        private String displayName = "";
        private String displayDesc = "";

        private Builder(Class<T> type, String rule, @NotNull T value) {
            if (type != value.getClass()) {
                // 基本数据类型和它们对应的包装类是不同的数据类型
                throw new IllegalArgumentException("Rule %s: type mismatch - expected %s, actual %s"
                        .formatted(rule, type.getSimpleName(), value.getClass().getSimpleName()));
            }
            this.type = type;
            this.value = value;
            this.name = rule;
            if (rule.isBlank()) {
                throw new IllegalArgumentException("Carpet rule name is empty");
            }
            if (this.type == Boolean.class) {
                // 不可变集合
                this.suggestions = List.of("true", "false");
            } else if (this.type.isEnum()) {
                // 不可变集合
                this.suggestions = Arrays.stream(this.type.getEnumConstants())
                        .map(e -> (Enum<?>) e)
                        .map(Enum::name)
                        .map(s -> s.toLowerCase(Locale.ROOT))
                        .toList();
            } else {
                this.suggestions = new ArrayList<>();
            }
            this.categories.add(CarpetOrgAdditionSettings.ORG);
        }

        public Builder<T> addCategories(String... categories) {
            return this.addCategories(List.of(categories));
        }

        public Builder<T> addCategories(List<String> list) {
            if (list.isEmpty()) {
                throw new IllegalArgumentException("At least one category must be provided");
            }
            this.categories.addAll(list);
            return this;
        }

        public Builder<T> addOptions(String... options) {
            return this.addOptions(List.of(options));
        }

        public Builder<T> addOptions(int... options) {
            return this.addOptions(Arrays.stream(options).mapToObj(Integer::toString).toList());
        }

        public Builder<T> addOptions(List<String> list) {
            if (list.isEmpty()) {
                throw new IllegalArgumentException("At least one option must be provided");
            }
            this.suggestions.addAll(list);
            return this;
        }

        public Builder<T> setClient() {
            this.canBeToggledClientSide = true;
            return this;
        }

        public Builder<T> setLenient() {
            this.strict = false;
            return this;
        }

        public Builder<T> setCustomRuleSwitch(Function<Boolean, T> customRuleValue, BooleanSupplier allowCustomSwitch) {
            this.control = new CustomRuleControl<>() {
                @Override
                public T getCustomRuleValue(ServerPlayer player) {
                    MinecraftServer server = ServerUtils.getServer(player);
                    ServerComponentCoordinator coordinator = ServerComponentCoordinator.getCoordinator(server);
                    CustomRuleValueManager ruleValueManager = coordinator.getCustomRuleValueManager();
                    boolean enabled = ruleValueManager.isEnabled(player, this);
                    return customRuleValue.apply(enabled);
                }

                @Override
                public boolean allowCustomSwitch() {
                    return allowCustomSwitch.getAsBoolean();
                }
            };
            return this;
        }

        public Builder<T> setDisplayName(String name) {
            this.displayName = name;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder<T> setDisplayDesc(String desc) {
            this.displayDesc = desc;
            return this;
        }

        public Builder<T> setHidden() {
            this.conditions.add(CarpetOrgAdditionConstants::isEnableHiddenFunction);
            this.addCategories(CarpetOrgAdditionSettings.HIDDEN);
            this.isHidden = true;
            return this;
        }

        public Builder<T> setRemoved() {
            this.conditions.addFirst(() -> false);
            this.isRemove = true;
            return this;
        }

        public Builder<T> addValidator(Predicate<T> predicate, Supplier<Component> supplier) {
            return this.addValidator(ValueValidator.of(predicate, supplier));
        }

        public final Builder<T> addValidator(ValueValidator<T> valueValidators) {
            this.validators.add(valueValidators);
            return this;
        }

        public final Builder<T> addSilenceValidator(SilenceValueValidator<T> observers) {
            this.silenceValidators.add(observers);
            return this;
        }

        public final Builder<T> addListener(RuleListener<T> listener) {
            this.listeners.add(listener);
            return this;
        }

        public RuleContext<T> build() {
            Supplier<CarpetRule<T>> supplier = () -> new OrgRule<>(
                    this.type,
                    this.name,
                    this.categories,
                    this.suggestions,
                    this.value,
                    this.canBeToggledClientSide,
                    this.validators,
                    this.silenceValidators,
                    this.listeners,
                    this.strict,
                    this.displayName,
                    this.displayDesc
            );
            return new RuleContext<>(
                    this.type,
                    this.value,
                    this.name,
                    supplier,
                    this.conditions,
                    this.categories,
                    this.suggestions,
                    this.isRemove,
                    this.isHidden,
                    this.control
            );
        }
    }
}
