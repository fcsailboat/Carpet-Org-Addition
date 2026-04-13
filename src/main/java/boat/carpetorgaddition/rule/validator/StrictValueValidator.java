package boat.carpetorgaddition.rule.validator;

import boat.carpetorgaddition.rule.ValidatorFeedbacks;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.RuleHelper;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public class StrictValueValidator<T> implements ValueValidator<T> {
    private final CarpetRule<T> rule;

    public StrictValueValidator(CarpetRule<T> rule) {
        this.rule = rule;
    }

    @Override
    public boolean validate(T newValue) {
        return this.rule.suggestions().contains(RuleHelper.toRuleString(newValue));
    }

    @Override
    public @NonNull Component errorMessage() {
        return ValidatorFeedbacks.validOptions(this.rule);
    }
}
