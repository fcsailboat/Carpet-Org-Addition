package boat.carpetorgaddition.rule;

import carpet.api.settings.CarpetRule;

import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class RuleContext<T> {
    private final Class<T> type;
    private final Collection<String> categories;
    private final Collection<String> suggestions;
    private final boolean isRemove;
    private final boolean isHidden;
    private final CustomRuleControl<T> control;
    private final T value;
    private final String name;
    private final Supplier<CarpetRule<T>> ruleSupplier;
    private final List<BooleanSupplier> conditions;
    private volatile CarpetRule<T> rule;

    public RuleContext(
            Class<T> type,
            T value,
            String name,
            Supplier<CarpetRule<T>> ruleSupplier,
            List<BooleanSupplier> conditions,
            Collection<String> categories,
            Collection<String> suggestions,
            boolean isRemove,
            boolean isHidden,
            CustomRuleControl<T> control
    ) {
        this.name = name;
        this.ruleSupplier = ruleSupplier;
        this.value = value;
        this.conditions = conditions;
        this.type = type;
        this.categories = categories;
        this.suggestions = suggestions;
        this.isRemove = isRemove;
        this.isHidden = isHidden;
        this.control = control;
    }

    public CarpetRule<T> rule() {
        // 在单人游戏中，初始化可能在客户端和服务端同时进行
        if (this.rule == null) {
            synchronized (this) {
                if (this.rule == null) {
                    this.rule = this.ruleSupplier.get();
                }
            }
        }
        return this.rule;
    }

    public T value() {
        return this.value;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isHidden() {
        return this.isHidden;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isRemove() {
        return this.isRemove;
    }

    public CustomRuleControl<T> getCustomRuleControl() {
        return this.control;
    }

    public String getName() {
        return this.name;
    }

    public Class<T> getType() {
        return type;
    }

    public Collection<String> getSuggestions() {
        return suggestions;
    }

    public Collection<String> getCategories() {
        return categories;
    }

    public boolean shouldRegister() {
        return this.conditions.stream().allMatch(BooleanSupplier::getAsBoolean);
    }
}
