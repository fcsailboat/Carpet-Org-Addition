package boat.carpetorgaddition.rule;

import net.minecraft.commands.CommandSourceStack;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface RuleListener<T> {
    /**
     * @param source 规则的修改者
     * @param value  规则的新值
     */
    void onChanged(@Nullable CommandSourceStack source, T value);
}
