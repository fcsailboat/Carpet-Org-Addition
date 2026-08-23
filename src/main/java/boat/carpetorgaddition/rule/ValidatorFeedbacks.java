package boat.carpetorgaddition.rule;

import boat.carpetorgaddition.wheel.common.CommonCommands;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys.Rule.Validate;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import carpet.api.settings.CarpetRule;
import carpet.api.settings.RuleHelper;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Objects;

public class ValidatorFeedbacks {
    private ValidatorFeedbacks() {
    }

    /**
     * 值必须大于%s
     */
    public static Component greaterThan(int number) {
        return Validate.GREATER_THAN.translate(number);
    }

    /**
     * 值必须小于%s
     */
    @SuppressWarnings("unused")
    public static Component lessThan(int number) {
        return Validate.LESS_THAN.translate(number);
    }

    /**
     * 值必须大于等于%s
     */
    public static Component greaterThanOrEqual(int number) {
        return Validate.GREATER_THAN_OR_EQUAL.translate(number);
    }

    /**
     * 值必须小于等于%s
     */
    public static Component lessThanOrEqual(int number) {
        return Validate.LESS_THAN_OR_EQUAL.translate(number);
    }

    /**
     * 值必须大于等于%s，或者为%s
     */
    public static Component greaterOrEqualOrValue(int number, int other) {
        return Validate.GREATER_THAN_OR_EQUAL_OR_NUMBER.translate(number, other);
    }

    /**
     * 值必须介于%s和%s之间，或者为%s
     */
    public static Component rangeOrValue(int number1, int number2, int other) {
        return Validate.BETWEEN_TWO_NUMBER_OR_NUMBER.translate(number1, number2, other);
    }

    /**
     * 有效选项: [%s1, %s2, ...]
     */
    public static <T> Component validOptions(CarpetRule<T> rule) {
        ArrayList<TextBuilder> list = new ArrayList<>();
        for (String suggestion : rule.suggestions()) {
            TextBuilder option = TextBuilder.of(suggestion)
                    .setHover(LocalizationKey.literal("carpet.settings.command.switch_to").translate(suggestion))
                    .setSuggestCommand(CommonCommands.setCarpetRule(rule.name(), suggestion));
            // 规则默认值设置为粗体
            if (Objects.equals(suggestion, RuleHelper.toRuleString(rule.defaultValue()))) {
                option.setBold();
            }
            // 规则当前值设置为斜体
            if (Objects.equals(suggestion, RuleHelper.toRuleString(rule.value()))) {
                option.setItalic();
            }
            list.add(option);
        }
        Component message = TextBuilder.joinList(list.stream().map(TextBuilder::build).toList(), TextBuilder.create(", "));
        return Validate.VALID_OPTIONS.translate(message);
    }
}
