package boat.carpetorgaddition.rule;

import net.minecraft.commands.CommandSourceStack;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface SilenceValueValidator<T> {
    /**
     * @param source 规则的修改者
     * @param value  规则的新值
     * @return 是否可以修改，如果为{@code false}，则阻止规则修改
     */
    boolean validate(@Nullable CommandSourceStack source, T value);
}
