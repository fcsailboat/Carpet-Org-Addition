package org.carpetorgaddition.rule.validator;

import carpet.api.settings.CarpetRule;
import carpet.api.settings.RuleHelper;
import carpet.api.settings.Validator;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.carpetorgaddition.util.MessageUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractValidator<T> extends Validator<T> {
    /**
     * @deprecated 不支持翻译
     */
    @Override
    @Deprecated(forRemoval = true)
    public final String description() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T validate(@Nullable ServerCommandSource serverCommandSource, CarpetRule<T> carpetRule, T newValue, String userInput) {
        T result = validate(newValue) ? newValue : null;
        onChange(serverCommandSource, result);
        return result;
    }

    /**
     * 规则的新值是否有效
     *
     * @param newValue 规则的新值
     * @return 如果值有效，返回true，否则返回false
     * @apiNote 与此方法重载方法一样不得抛出异常
     */
    public abstract boolean validate(T newValue);

    /**
     * @return 规则校验失败时的错误反馈
     */
    @NotNull
    public abstract Text errorMessage();

    @Override
    public void notifyFailure(ServerCommandSource source, CarpetRule<T> currentRule, String providedValue) {
        // 获取此规则的翻译名称
        String translatedName = RuleHelper.translatedName(currentRule);
        MessageUtils.sendErrorMessage(source, "carpet.rule.validate.invalid_value", translatedName, providedValue);
        MessageUtils.sendErrorMessage(source, errorMessage());
    }

    /**
     * 当规则被更改时调用
     *
     * @param source   规则值的修改者，如果在规则同步期间调用，可能为{@code null}
     * @param newValue 规则的新值，如果为{@code null}，表示规则没有被修改
     */
    public void onChange(@Nullable ServerCommandSource source, @Nullable T newValue) {
    }
}
