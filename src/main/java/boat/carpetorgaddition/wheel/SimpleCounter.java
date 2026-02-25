package boat.carpetorgaddition.wheel;

import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 计数器
 *
 * @param <E> 计数器要统计数量的类
 */
public class SimpleCounter<E> implements Counter<E> {
    private final Object2IntMap<E> counter = new Object2IntArrayMap<>();

    public SimpleCounter() {
    }

    /**
     * 递增元素数量
     */
    public void increment(E element) {
        this.add(element, 1);
    }

    /**
     * 递减元素数量
     */
    public void decrement(E element) {
        this.subtract(element, 1);
    }

    @Override
    public void add(E element, int count) {
        int i = this.counter.getInt(element);
        this.set(element, i + count);
    }

    /**
     * 将集合内一个元素的数量设置为指定值
     *
     * @param element 要修改数量的元素
     * @param count   修改后的数量
     */
    @Override
    public void set(E element, int count) {
        if (count == 0) {
            // TODO 测试是否可以正确删除元素
            this.counter.removeInt(element);
        } else {
            this.counter.put(element, count);
        }
    }

    /**
     * 获取一个元素在集合内的数量
     *
     * @param element 要获取数量的元素
     * @return 此元素在计数器内的数量
     */
    @Override
    public int getCount(E element) {
        return this.counter.getInt(element);
    }

    /**
     * 获取集合内总共有多少种元素
     *
     * @return 集合的大小
     */
    @Override
    public int size() {
        return counter.size();
    }

    /**
     * @return 当前集合是否为空
     */
    @Override
    public boolean isEmpty() {
        return this.counter.isEmpty();
    }

    @Override
    public Stream<Object2IntMap.Entry<E>> stream() {
        return this.counter.object2IntEntrySet().stream();
    }

    @Override
    public Set<E> keySet() {
        return this.counter.keySet();
    }

    @Override
    public Set<Object2IntMap.Entry<E>> entrySet() {
        return this.counter.object2IntEntrySet();
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        return new Iterator<>() {
            private final ObjectIterator<Object2IntMap.Entry<E>> it = counter.object2IntEntrySet().iterator();

            @Override
            public boolean hasNext() {
                return this.it.hasNext();
            }

            @Override
            public E next() {
                return this.it.next().getKey();
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
    @Deprecated
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
            if (obj instanceof SimpleCounter.Wrapper<?> wrapper) {
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
