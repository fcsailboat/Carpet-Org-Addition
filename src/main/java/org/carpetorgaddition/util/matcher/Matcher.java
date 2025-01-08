package org.carpetorgaddition.util.matcher;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.Predicate;

@Deprecated(forRemoval = true)
public interface Matcher extends Predicate<ItemStack> {
    /**
     * 当前物品是否与空气物品匹配
     *
     * @return 匹配器的内容是否为空
     */
    boolean isEmpty();

    /**
     * 判断当前匹配器的内容物是否为物品
     *
     * @return 是物品返回true，是物品标签返回false
     */
    boolean isItem();

    /**
     * 获取匹配器内的物品
     *
     * @return 如果是物品直接返回，如果是物品标签返回空气物品
     */
    Item getItem();

    /**
     * 返回匹配器的名称
     *
     * @return 如果是物品，返回物品的名称，如果是物品标签，返回物品标签的字符串
     */
    Text getName();

    /**
     * 返回此匹配器的可变文本形式
     *
     * @return 如果是物品，返回默认堆栈的{@link ItemStack#toHoverableText()}，如果是物品标签，返回物品标签字符串的可变文本形式
     */
    default MutableText toText() {
        return this.getName().copy();
    }

    /**
     * 根据物品id获取对应物品
     *
     * @param id 物品的命名空间和id
     * @return 指定的物品
     */
    static Item asItem(String id) {
        String[] split = id.strip().split(":");
        Identifier identifier = (split.length == 1
                ? Identifier.of(Identifier.DEFAULT_NAMESPACE, split[0])
                : Identifier.of(split[0], split[1]));
        return Registries.ITEM.get(identifier);
    }
}
