package org.carpetorgaddition.util.matcher;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/**
 * 此物品匹配器尝试匹配物品时，只检查目标物品堆栈对应的物品是否与该物品匹配器内包含的物品相同，不考虑物品的NBT
 */
@Deprecated(forRemoval = true)
public class ItemMatcher implements Matcher {
    private final Item item;

    public ItemMatcher(Item item) {
        this.item = item;
    }

    public ItemMatcher(ItemStack itemStack) {
        this.item = itemStack.getItem();
    }

    @Override
    public boolean test(ItemStack itemStack) {
        return itemStack.isOf(item);
    }

    @Override
    public boolean isEmpty() {
        return this.item == Items.AIR;
    }

    @Override
    public boolean isItem() {
        return true;
    }

    @Override
    public Item getItem() {
        return this.item;
    }

    @Override
    public Text getName() {
        return this.item.getName();
    }

    @Override
    public MutableText toText() {
        return this.item.getDefaultStack().toHoverableText().copy();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof ItemMatcher itemMatcher) {
            return this.item == itemMatcher.item;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.item.hashCode();
    }

    @Override
    public String toString() {
        return Registries.ITEM.getId(this.item).toString();
    }
}
