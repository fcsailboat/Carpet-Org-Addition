package org.carpetorgaddition.rule.validator;

import net.minecraft.text.MutableText;
import org.carpetorgaddition.util.constant.RuleValidatorConstants;
import org.jetbrains.annotations.NotNull;

// 最大方块交互距离校验
public class MaxBlockPlaceDistanceValidator extends AbstractValidator<Double> {
    /**
     * 最大方块交互距离
     */
    public static final double MAX_VALUE = 256.0;

    @Override
    public boolean validate(Double newValue) {
        return (newValue >= 0 && newValue <= MAX_VALUE) || newValue == -1;
    }

    @Override
    public @NotNull MutableText errorMessage() {
        return RuleValidatorConstants.betweenTwoNumberOrNumber(0, (int) MAX_VALUE, -1);
    }
}
