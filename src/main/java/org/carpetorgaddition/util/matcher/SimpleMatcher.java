package org.carpetorgaddition.util.matcher;

import net.minecraft.item.ItemStack;

import java.util.function.Predicate;

@Deprecated(forRemoval = true)
@FunctionalInterface
public interface SimpleMatcher extends Predicate<ItemStack> {
    /**
     * 检查当前物品堆栈是否与匹配器匹配
     *
     * @param itemStack 要匹配的物品堆栈
     * @return 当前物品堆栈是否与匹配器匹配
     */
    boolean test(ItemStack itemStack);
}
