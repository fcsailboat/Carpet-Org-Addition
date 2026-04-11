package boat.carpetorgaddition.rule;

import boat.carpetorgaddition.CarpetOrgAdditionConstants;
import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.rule.validator.ValueValidator;
import boat.carpetorgaddition.util.ServerUtils;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.RuleCategory;
import carpet.utils.CommandHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class RuleFactory {
    @Deprecated(forRemoval = true)
    public static <T> Builder<T> create(Class<T> type, String rule, T value) {
        return new Builder<>(type, rule, value);
    }

    public static Builder<Integer> create(String rule, int value) {
        return new Builder<>(Integer.class, rule, value);
    }

    public static Builder<Long> create(String rule, long value) {
        return new Builder<>(Long.class, rule, value);
    }

    public static Builder<Boolean> create(String rule, boolean value) {
        return new Builder<>(Boolean.class, rule, value);
    }

    public static Builder<Float> create(String rule, float value) {
        return new Builder<>(Float.class, rule, value);
    }

    public static Builder<Double> create(String rule, double value) {
        return new Builder<>(Double.class, rule, value);
    }

    public static Builder<String> create(String rule, String value) {
        return new Builder<>(String.class, rule, value);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> Builder<T> create(String rule, Enum<T> value) {
        return new Builder<>(value.getDeclaringClass(), rule, (T) value);
    }

    public static final class Builder<T> {
        private final Class<T> type;
        private final String name;
        private final Collection<String> categories = new ArrayList<>();
        private final Collection<String> suggestions;
        @NotNull
        private final T value;
        private final List<ValueValidator<T>> validators = new ArrayList<>();
        private final List<SilenceValueValidator<T>> silenceValidators = new ArrayList<>();
        private final List<RuleListener<T>> listeners = new ArrayList<>();
        private final List<BooleanSupplier> conditions = new ArrayList<>();
        private boolean client = false;
        private boolean strict = true;
        private boolean hidden;
        private boolean removed;
        @Nullable
        private CustomRuleControl<T> control;
        private String displayName = "";
        private String displayDesc = "";

        private Builder(Class<T> type, String rule, @NotNull T value) {
            if (type != value.getClass()) {
                // 基本数据类型和它们对应的包装类是不同的数据类型
                throw new IllegalArgumentException("Rule %s: type mismatch - expected %s, actual %s"
                        .formatted(rule, type.getSimpleName(), value.getClass().getSimpleName()));
            }
            if (rule.isBlank()) {
                throw new IllegalArgumentException("Carpet rule name is empty");
            }
            this.type = type;
            this.value = value;
            this.name = rule;
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
            this.categories.addAll(Arrays.asList(categories));
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

        /**
         * 设置此规则为命令的开关，当规则值切换时，会将命令更改同步到客户端
         */
        public Builder<T> setCommand() {
            this.categories.add(RuleCategory.COMMAND);
            // 更改规则时将命令同步到客户端
            return this.addListener((source, _) -> {
                if (source != null) {
                    CommandHelper.notifyPlayersCommandsChanged(source.getServer());
                }
            });
        }

        /**
         * 设置为{@code Client}的规则，可以在未安装{@code Carpet}的服务器中通过{@code /carpet}或对应规则管理器的命令启用（尽管客户端无法解析命令）
         *
         * @see CarpetRule#canBeToggledClientSide()
         */
        public Builder<T> setClient() {
            this.client = true;
            this.categories.add(RuleCategory.CLIENT);
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

        /**
         * 将规则标记为隐藏，除非启用隐藏功能，否则隐藏的规则不会在游戏中显示
         */
        public Builder<T> setHidden() {
            this.conditions.add(CarpetOrgAdditionConstants::isEnableHiddenFunction);
            this.addCategories(CarpetOrgAdditionSettings.HIDDEN);
            this.hidden = true;
            return this;
        }

        /**
         * 将规则标记为已删除，已删除的规则不会在游戏中显示
         */
        public Builder<T> setRemoved() {
            this.conditions.addFirst(() -> false);
            this.removed = true;
            return this;
        }

        /**
         * 添加规则验证器
         *
         * @param condition    用于验证规则值是否有效的条件
         * @param errorMessage 规则值无效时，发送的命令反馈消息
         */
        public Builder<T> addValidator(Predicate<T> condition, Supplier<Component> errorMessage) {
            return this.addValidator(ValueValidator.of(condition, errorMessage));
        }

        public Builder<T> addValidator(ValueValidator<T> validator) {
            this.validators.add(validator);
            return this;
        }

        @SuppressWarnings("unused")
        public Builder<T> addSilenceValidator(SilenceValueValidator<T> observers) {
            this.silenceValidators.add(observers);
            return this;
        }

        public Builder<T> addListener(RuleListener<T> listener) {
            this.listeners.add(listener);
            return this;
        }

        public RuleContext<T> build() {
            Supplier<CarpetRule<T>> supplier = () -> new BuiltRule<>(
                    this.type,
                    this.name,
                    this.categories,
                    this.suggestions,
                    this.value,
                    this.client,
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
                    this.removed,
                    this.hidden,
                    this.control
            );
        }
    }
}
