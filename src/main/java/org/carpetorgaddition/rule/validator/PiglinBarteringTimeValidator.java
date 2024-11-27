package org.carpetorgaddition.rule.validator;

import net.minecraft.text.MutableText;
import org.carpetorgaddition.util.constant.RuleValidatorConstants;
import org.jetbrains.annotations.NotNull;

// 自定义猪灵交易时间
public class PiglinBarteringTimeValidator extends AbstractValidator<Long> {
    @Override
    public boolean validate(Long newValue) {
        return newValue >= 0 || newValue == -1;
    }

    @Override
    public @NotNull MutableText errorMessage() {
        return RuleValidatorConstants.greaterThanOrEqualOrNumber(0, -1);
    }
}
