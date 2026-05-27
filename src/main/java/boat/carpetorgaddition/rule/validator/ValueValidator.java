package boat.carpetorgaddition.rule.validator;

import boat.carpetorgaddition.rule.RuleUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import carpet.api.settings.CarpetRule;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.function.Predicate;
import java.util.function.Supplier;

public interface ValueValidator<T> {
    /**
     * 规则的新值是否有效
     *
     * @param newValue 规则的新值
     * @return 如果值有效，返回true，否则返回false
     * @apiNote 与此方法重载方法一样不得抛出异常
     */
    boolean validate(T newValue);

    /**
     * @return 规则校验失败时的错误反馈
     */
    @NonNull
    Component errorMessage();


    default void notifyFailure(CommandSourceStack source, CarpetRule<T> currentRule, String providedValue) {
        // 获取此规则的翻译名称
        Component name = RuleUtils.simpleTranslationName(currentRule);
        MessageUtils.sendErrorMessage(source, LocalizationKeys.Rule.Validate.INVALID_VALUE.translate(name, providedValue));
        MessageUtils.sendErrorMessage(source, errorMessage());
    }

    static <T> ValueValidator<T> of(Predicate<T> predicate, Supplier<Component> supplier) {
        return new ValueValidator<>() {
            @Override
            public boolean validate(T newValue) {
                return predicate.test(newValue);
            }

            @Override
            public @NonNull Component errorMessage() {
                return supplier.get();
            }
        };
    }
}
