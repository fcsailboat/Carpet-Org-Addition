package org.carpetorgaddition.rule.validator;

import net.minecraft.text.Text;
import org.carpetorgaddition.util.constant.RuleValidatorConstants;
import org.jetbrains.annotations.NotNull;

public class FinderCommandMaxFeedbackCountValidator extends AbstractValidator<Integer> {
    @Override
    public boolean validate(Integer newValue) {
        return newValue > 0;
    }

    @Override
    public @NotNull Text errorMessage() {
        return RuleValidatorConstants.greaterThan(0);
    }
}
