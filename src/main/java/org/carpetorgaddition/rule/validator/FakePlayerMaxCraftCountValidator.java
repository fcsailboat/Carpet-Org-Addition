package org.carpetorgaddition.rule.validator;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.carpetorgaddition.util.constant.RuleValidatorConstants;
import org.jetbrains.annotations.NotNull;

public class FakePlayerMaxCraftCountValidator extends AbstractValidator<Integer> {
    /**
     * 最小合成次数
     */
    public static final int MIN_CRAFT_COUNT = 1;

    @Override
    public boolean validate(Integer newValue) {
        return newValue >= MIN_CRAFT_COUNT || newValue == -1;
    }

    @Override
    public @NotNull Text errorMessage() {
        return RuleValidatorConstants.greaterThanOrEqualOrNumber(MIN_CRAFT_COUNT, -1);
    }
}
