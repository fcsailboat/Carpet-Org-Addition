package org.carpetorgaddition.util;

import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.carpetorgaddition.util.inventory.ImmutableInventory;
import org.carpetorgaddition.util.matcher.SimpleMatcher;
import org.carpetorgaddition.util.wheel.ContainerDeepCopy;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class InventoryUtils {
    /**
     * 物品栏工具类，私有化构造方法
     */
    private InventoryUtils() {
    }

    /**
     * 从物品形式的潜影盒中获取第一个指定的物品，并将该物品从潜影盒的NBT中删除，使用时，为避免不必要的物品浪费，取出来的物品必须使用或丢出
     *
     * @param shulkerBox 潜影盒物品
     * @param matcher    一个物品匹配器对象，用来指定要从潜影盒中拿取的物品
     * @return 潜影盒中获取的指定物品
     */
    public static ItemStack pickItemFromShulkerBox(ItemStack shulkerBox, SimpleMatcher matcher) {
        // 判断潜影盒是否为空，空潜影盒直接返回空物品
        if (isEmptyShulkerBox(shulkerBox)) {
            return ItemStack.EMPTY;
        }
        // 将潜影盒内的物品栏组件替换为该组件的深拷贝副本
        InventoryUtils.deepCopyContainer(shulkerBox);
        ContainerComponent component = shulkerBox.get(DataComponentTypes.CONTAINER);
        //noinspection DataFlowIssue
        for (ItemStack itemStack : component.iterateNonEmpty()) {
            if (matcher.test(itemStack)) {
                ItemStack copy = itemStack.copy();
                itemStack.setCount(0);
                ifItIsEmptyRemoveIt(itemStack);
                return copy;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * 获取潜影盒中指定物品，并让这个物品执行一个函数，然后将执行函数前的物品返回
     *
     * @param matcher  匹配物品的谓词
     * @param consumer 要执行的函数
     * @return 执行函数前的物品
     */
    public static ItemStack shulkerBoxConsumer(ItemStack shulkerBox, SimpleMatcher matcher, Consumer<ItemStack> consumer) {
        if (isEmptyShulkerBox(shulkerBox)) {
            // 因为这个判断，可以保证下方的shulkerBox.get(DataComponentTypes.CONTAINER)不会返回null
            return ItemStack.EMPTY;
        }
        ContainerComponent component = shulkerBox.get(DataComponentTypes.CONTAINER);
        // noinspection DataFlowIssue
        for (ItemStack stack : component.iterateNonEmpty()) {
            if (matcher.test(stack)) {
                ItemStack copyStack = stack.copy();
                consumer.accept(stack);
                return copyStack;
            }
        }
        return ItemStack.EMPTY;
    }

    // 如果潜影盒为空，删除对应的NBT
    private static void ifItIsEmptyRemoveIt(ItemStack shulkerBox) {
        if (isEmptyShulkerBox(shulkerBox)) {
            // 如果潜影盒最后一个物品被取出，就删除潜影盒的物品栏数据堆叠组件以保证潜影盒堆叠的正常运行
            shulkerBox.set(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT);
        }
    }

    /**
     * 判断当前潜影盒是否是空潜影盒
     *
     * @param shulkerBox 当前要检查是否为空的潜影盒物品
     * @return 潜影盒内没有物品返回true，有物品返回false
     * @apiNote 此方法可以保证返回值为false时，shulkerBox.get(DataComponentTypes.CONTAINER)永远不会返回null
     */
    public static boolean isEmptyShulkerBox(ItemStack shulkerBox) {
        // 正常情况下有物品的潜影盒无法堆叠
        if (shulkerBox.getCount() != 1) {
            return true;
        }
        ContainerComponent component = shulkerBox.get(DataComponentTypes.CONTAINER);
        if (component == null) {
            return true;
        }
        return !component.iterateNonEmpty().iterator().hasNext();
    }

    /**
     * 获取潜影盒物品的物品栏
     *
     * @param shulkerBox 要获取物品栏的潜影盒
     * @return 潜影盒内的物品栏
     */
    public static ImmutableInventory getInventory(ItemStack shulkerBox) {
        if (isEmptyShulkerBox(shulkerBox)) {
            return ImmutableInventory.EMPTY;
        }
        // 获取潜影盒NBT
        ContainerComponent component = shulkerBox.get(DataComponentTypes.CONTAINER);
        // 因为有空潜影盒的判断，shulkerBox.get(DataComponentTypes.CONTAINER)不会返回null
        //noinspection DataFlowIssue
        return new ImmutableInventory(component.streamNonEmpty().toList());
    }


    /**
     * 在创造模式下使用鼠标中键复制的物品时，物品组件只是被浅拷贝了，这些被复制的物品还是共享同一个组件地址，当直接对其中一个组件进行操作时，所有被复制的物品都会受到影响，换句话说，当其中一个潜影盒中的物品被本类中的方法取出来后，所有被复制的潜影盒中这个物品都会消失，假玩家也就不能正确的从潜影盒中拿取物品。所以本方法的作用是将物品组件替换为它的深克隆对象。
     *
     * @param shulkerBox 要替换组件的潜影盒
     * @see <a href="https://bugs.mojang.com/browse/MC-271123">MC-271123</a>
     */
    public static void deepCopyContainer(ItemStack shulkerBox) {
        ContainerComponent component = shulkerBox.get(DataComponentTypes.CONTAINER);
        if (component == null) {
            return;
        }
        ContainerComponent copy = ((ContainerDeepCopy) (Object) component).copy();
        shulkerBox.set(DataComponentTypes.CONTAINER, copy);
    }

    /**
     * 判断指定物品是否为潜影盒
     *
     * @param shulkerBox 要判断是否为潜影盒的物品
     * @return 指定物品是否是潜影盒
     */
    public static boolean isShulkerBoxItem(ItemStack shulkerBox) {
        if (shulkerBox.isOf(Items.SHULKER_BOX)) {
            return true;
        }
        if (shulkerBox.getItem() instanceof BlockItem blockItem) {
            return blockItem.getBlock() instanceof ShulkerBoxBlock;
        }
        return false;
    }

    /**
     * 断言指定物品为空
     *
     * @param itemStack 被断言的物品
     */
    public static void assertEmptyStack(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return;
        }
        throw new IllegalStateException();
    }

    /**
     * 断言指定物品为空
     *
     * @param itemStack 被断言的物品
     * @param message   异常消息
     */
    public static void assertEmptyStack(ItemStack itemStack, Supplier<String> message) {
        if (itemStack.isEmpty()) {
            return;
        }
        throw new IllegalStateException(message.get());
    }
}
