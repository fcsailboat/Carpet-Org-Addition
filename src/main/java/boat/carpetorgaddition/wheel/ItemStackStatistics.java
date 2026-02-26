package boat.carpetorgaddition.wheel;

import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.function.Predicate;

/**
 * 用来统计一个物品栏内的物品数量
 */
public class ItemStackStatistics {
    /**
     * 统计到的物品总数
     */
    private int sum;
    /**
     * 物品谓词
     */
    private final Predicate<ItemStack> predicate;
    /**
     * 每一种物品的数量
     */
    private final Counter<Item> counter = new SimpleCounter<>();
    /**
     * 用来记录哪些物品是（或有些是）从嵌套的物品栏中找到的
     */
    private final HashSet<Item> nestingItem = new HashSet<>();

    public ItemStackStatistics(Predicate<ItemStack> predicate) {
        this.predicate = predicate;
    }

    /**
     * 统计物品栏内指定物品的数量<br>
     * 如果物品栏内包含容器物品或者收纳袋，则同时统计嵌套的物品数量
     */
    public void statistics(Container inventory) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            this.tally(inventory.getItem(i));
        }
    }

    private void statistics(Iterable<ItemStack> iterable, int multiple) {
        iterable.forEach(itemStack -> this.tally(itemStack, true, multiple));
    }

    private void statistics(ItemStack itemStack) {
        // 容器物品
        int count = itemStack.getCount();
        ItemContainerContents container = itemStack.get(DataComponents.CONTAINER);
        if (container != null) {
            this.statistics(container.nonEmptyItemCopyStream().toList(), count);
            // 不考虑一个物品同时有容器物品组件和收纳袋物品组件的情况
            return;
        }
        // 收纳袋物品
        BundleContents bundleContents = itemStack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundleContents != null) {
            this.statistics(bundleContents.itemCopyStream().toList(), count);
        }
    }

    /**
     * 累加该物品的数量
     *
     * @param itemStack 物品和物品的数量
     */
    private void tally(ItemStack itemStack) {
        this.tally(itemStack, false, 1);
    }

    /**
     * 累加该物品的数量
     *
     * @param itemStack 物品和物品的数量
     * @param nesting   该物品是否是从嵌套的容器中获取的
     * @param multiple  数量的倍数，用于堆叠的容器物品
     */
    private void tally(ItemStack itemStack, boolean nesting, int multiple) {
        if (this.predicate.test(itemStack)) {
            Item item = itemStack.getItem();
            int count = itemStack.getCount();
            this.counter.add(item, count * multiple);
            this.sum += (count * multiple);
            if (nesting) {
                this.nestingItem.add(item);
            }
        }
        this.statistics(itemStack);
    }

    public int getSum() {
        return this.sum;
    }

    public boolean hasNestingItem() {
        return !this.nestingItem.isEmpty();
    }

    public Component getCountText() {
        ArrayList<Component> list = new ArrayList<>();
        for (Item item : this.counter) {
            Component itemCount = itemCount(this.counter.getCount(item), item.getDefaultMaxStackSize());
            if (this.nestingItem.contains(item)) {
                TextBuilder builder = TextBuilder.fromCombined(ServerUtils.getName(item), " ", itemCount);
                builder.setItalic();
                list.add(builder.build());
            } else {
                list.add(TextBuilder.combineAll(ServerUtils.getName(item), " ", itemCount));
            }
        }
        Component text = TextBuilder.create(this.getSum());
        TextBuilder builder = TextBuilder.of(text);
        builder.setHover(TextBuilder.joinList(list));
        return this.nestingItem.isEmpty() ? builder.build() : builder.setItalic().build();
    }

    private Component itemCount(int count, int maxCount) {
        // 计算物品有多少组
        int group = count / maxCount;
        // 计算物品余几个
        int remainder = count % maxCount;
        // 为文本添加悬停提示
        if (group == 0) {
            return LocalizationKeys.Item.REMAINDER.translate(remainder);
        } else if (remainder == 0) {
            return LocalizationKeys.Item.GROUP.translate(group);
        } else {
            return LocalizationKeys.Item.COUNT.translate(group, remainder);
        }
    }
}
