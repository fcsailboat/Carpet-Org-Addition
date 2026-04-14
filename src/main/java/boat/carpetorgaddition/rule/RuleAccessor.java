package boat.carpetorgaddition.rule;

import boat.carpetorgaddition.CarpetOrgAdditionExtension;
import boat.carpetorgaddition.mixin.accessor.carpet.SettingsManagerAccessor;
import carpet.api.settings.CarpetRule;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.function.Supplier;

public final class RuleAccessor<T> {
    private final Supplier<T> value;
    private final BuiltRule<T> rule;
    private final String key;
    @Nullable
    private final CustomRuleControl<T> control;

    public RuleAccessor(RuleContext<T> context) {
        this.value = () -> (CarpetOrgAdditionExtension.getInstance().isCarpetRuleLoaded() ? context.rule().value() : context.value());
        this.rule = context.rule();
        this.key = this.rule.name();
        this.control = context.getCustomRuleControl();
    }

    public T value() {
        return this.value.get();
    }

    public T value(@NonNull ServerPlayer player) {
        if (this.control != null && this.control.allowCustomSwitch()) {
            return this.control.getCustomRuleValue(player);
        }
        return this.value();
    }

    @SuppressWarnings("unused")
    public CarpetRule<T> getCarpetRule() {
        return this.rule;
    }

    public Component getDisplayName() {
        return RuleUtils.simpleTranslationName(this.rule);
    }

    public String getKey() {
        return this.key;
    }

    @SuppressWarnings("unused")
    public boolean isEnabled() {
        return this.rule.isEnable();
    }

    public boolean isDisabled() {
        return !this.rule.isEnable();
    }

    @SuppressWarnings("unused")
    public void setRuleValue(CommandSourceStack source, T value) {
        SettingsManagerAccessor accessor = (SettingsManagerAccessor) this.rule.settingsManager();
        accessor.changeRuleValue(source, this.rule, this.valueAsString(value));
    }

    private String valueAsString(T value) {
        if (value.getClass().isEnum()) {
            return ((Enum<?>) value).name().toLowerCase(Locale.ROOT);
        }
        return value.toString();
    }
}
