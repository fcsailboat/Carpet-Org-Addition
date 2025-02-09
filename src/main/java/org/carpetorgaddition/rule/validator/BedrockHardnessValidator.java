package org.carpetorgaddition.rule.validator;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.carpetorgaddition.util.constant.RuleValidatorConstants;
import org.jetbrains.annotations.NotNull;

// 设置基岩硬度校验
public class BedrockHardnessValidator extends AbstractValidator<Float> {
    private BedrockHardnessValidator() {
    }

    @Override
    public boolean validate(Float newValue) {
        return newValue >= 0 || newValue == -1;
    }

    @Override
    public @NotNull Text errorMessage() {
        return RuleValidatorConstants.greaterThanOrEqualOrNumber(0, -1);
    }
}
