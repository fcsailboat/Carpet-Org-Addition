package org.carpetorgaddition.rule.validator;

import net.minecraft.text.MutableText;
import org.carpetorgaddition.util.constant.RuleValidatorConstants;
import org.jetbrains.annotations.NotNull;

// 信标范围验证
public class BeaconRangeExpandValidator extends AbstractValidator<Integer> {
    public static final int MAX_VALUE = 1024;

    @Override
    public boolean validate(Integer integer) {
        return integer <= MAX_VALUE;
    }

    @Override
    public @NotNull MutableText errorMessage() {
        return RuleValidatorConstants.lessThanOrEqual(MAX_VALUE);
    }
}
