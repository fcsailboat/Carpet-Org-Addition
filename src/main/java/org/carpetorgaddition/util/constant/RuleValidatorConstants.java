package org.carpetorgaddition.util.constant;

import net.minecraft.text.MutableText;
import org.carpetorgaddition.util.TextUtils;

public class RuleValidatorConstants {
    /**
     * 值必须小于等于%s
     */
    public static MutableText lessThanOrEqual(int number) {
        return TextUtils.translate("carpet.rule.validate.less_than_or_equal", number);
    }

    /**
     * 值必须大于等于%s，或者为%s
     */
    public static MutableText greaterThanOrEqualOrNumber(int number, int other) {
        return TextUtils.translate("carpet.rule.validate.greater_than_or_equal_or_number", number, other);
    }

    /**
     * 值必须介于%s和%s之间，或者为%s
     */
    public static MutableText betweenTwoNumberOrNumber(int number1, int number2, int other) {
        return TextUtils.translate("carpet.rule.validate.between_two_number_or_number", number1, number2, other);
    }
}
