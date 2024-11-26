package org.carpetorgaddition.util.wheel;

import net.minecraft.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Stream;

/**
 * 虽然在大多数情况下使用int count = 0; count++;的方式能够解决统计数量的问题，但是这种方式有局限性，即一次只能统计一种事物的数量，当要同时统计多种事物的数量，或者根本就不知道有多少种事物要被统计时使用这种方式就非常不方便，举个例子，如果需要统计当前Minecraft世界中上每一种生物的数量，我们并不知道当前世界上一共有多少种生物要被统计，也就无法知道要到底定义几个计数器，这时就可以使用本类的计数器，它实际上包装了一个HashMap集合，利用向集合中添加元素时，如果元素是相同的，元素不会重复添加而是覆盖的特性来统计事物的数量。另外，本类实现了Iterable接口，因此，本类可以使用迭代器和增强for循环遍历
 *
 * @param <E> 计数器要统计数量的类
 */
public class Counter<E> implements Iterable<E> {
    private final HashMap<E, Integer> counter = new HashMap<>();

    public Counter() {
    }

    /**
     * 使集合内一个元素的数量递增
     *
     * @param element 要数量递增的元素
     */
    public void add(E element) {
        add(element, 1);
    }

    /**
     * 使集合内元素的数量递减
     *
     * @param element 要数量递减的元素
     */
    @SuppressWarnings("unused")
    public void decrement(E element) {
        add(element, -1);
    }

    /**
     * 将一个元素的数量增加指定值，可以是正数，也可以是负数，表示减少指定值
     *
     * @param element 要增加数量的元素
     * @param count   增加的个数
     */
    @SuppressWarnings("Java8MapApi")
    public void add(E element, int count) {
        Integer i = this.counter.get(element);
        this.counter.put(element, i == null ? count : i + count);
    }

    /**
     * 将集合内一个元素的数量设置为指定值
     *
     * @param element 要修改数量的元素
     * @param count   修改后的数量
     */
    public void set(E element, int count) {
        this.counter.put(element, count);
    }

    /**
     * 获取一个元素在集合内的数量
     *
     * @param element 要获取数量的元素
     * @return 此元素在计数器内的数量
     */
    public int getCount(E element) {
        Integer i = this.counter.get(element);
        return i == null ? 0 : i;
    }

    /**
     * 获取集合内总共有多少种元素
     *
     * @return 集合的大小
     */
    public int size() {
        return counter.size();
    }

    /**
     * @return 当前集合是否为空
     */
    public boolean isEmpty() {
        return this.counter.isEmpty();
    }

    /**
     * 判断一个元素在集合内是否存在
     *
     * @param element 要判断是否存在的元素
     * @return 指定元素是否在集合内的数量大于0
     */
    @SuppressWarnings("unused")
    public boolean hasElement(E element) {
        return this.getCount(element) > 0;
    }

    public Stream<E> stream() {
        return this.counter.keySet().stream();
    }

    @SuppressWarnings("unused")
    public List<Pair<E, Integer>> sort() {
        return this.stream()
                .sorted(Comparator.comparingInt(element -> -Counter.this.getCount(element)))
                .map(e -> new Pair<>(e, this.getCount(e)))
                .toList();
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        return new Iterator<>() {
            private final Iterator<Map.Entry<E, Integer>> iterator = counter.entrySet().iterator();

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public E next() {
                return iterator.next().getKey();
            }
        };
    }

    @Override
    public String toString() {
        return counter.toString();
    }

    /**
     * 计数器是通过对象内的equals和hashCode方法来判断是否为相同的事物的，但是并不是所有对象equals都能满足当前的需要，
     * 因此可以使用本类包装并根据实际情况来重写这个内部类中的方法
     */
    @SuppressWarnings("unused")
    public abstract static class Wrapper<T> {
        private final T value;

        public Wrapper(T value) {
            this.value = value;
        }

        public final T getValue() {
            return this.value;
        }

        @Override
        public final boolean equals(Object obj) {
            if (obj instanceof Counter.Wrapper<?> wrapper) {
                return this.valueEquals(this.value, wrapper.value);
            }
            return false;
        }

        @Override
        public final int hashCode() {
            return this.valueHashCode(value);
        }

        /**
         * 比较两个包装器内的值是否相等
         *
         * @param value1 当前包装器内的值
         * @param value2 另一个包装器内的值，但它需要手动强制成对应的数据类型
         */
        public abstract boolean valueEquals(T value1, Object value2);

        /**
         * 获取当前包装器内值的哈希值，按照约定，只要重写了equals方法，都应该重写hashCode方法，相同的对象需要有相同的哈希值
         *
         * @param value 当前包装器内的值
         */
        public abstract int valueHashCode(T value);

        @Override
        public String toString() {
            return value.toString();
        }
    }
}
