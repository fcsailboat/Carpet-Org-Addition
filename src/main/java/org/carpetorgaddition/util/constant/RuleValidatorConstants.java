package org.carpetorgaddition.util.constant;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.carpetorgaddition.util.TextUtils;

@SuppressWarnings("unused")
public class RuleValidatorConstants {
    /**
     * 值必须大于%s
     */
    public static Text greaterThan(int number) {
        return TextUtils.translate("carpet.rule.validate.greater_than", number);
    }

    /**
     * 值必须小于%s
     */
    public static Text lessThan(int number) {
        return TextUtils.translate("carpet.rule.validate.less_than", number);
    }

    /**
     * 值必须大于等于%s
     */
    public static Text greaterThanOrEqual(int number) {
        return TextUtils.translate("carpet.rule.validate.greater_than_or_equal", number);
    }

    /**
     * 值必须小于等于%s
     */
    public static Text lessThanOrEqual(int number) {
        return TextUtils.translate("carpet.rule.validate.less_than_or_equal", number);
    }

    /**
     * 值必须大于等于%s，或者为%s
     */
    public static Text greaterThanOrEqualOrNumber(int number, int other) {
        return TextUtils.translate("carpet.rule.validate.greater_than_or_equal_or_number", number, other);
    }

    /**
     * 值必须介于%s和%s之间，或者为%s
     */
    public static Text betweenTwoNumberOrNumber(int number1, int number2, int other) {
        return TextUtils.translate("carpet.rule.validate.between_two_number_or_number", number1, number2, other);
    }
}
