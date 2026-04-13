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

    @Override
    public Stream<Object2IntMap.Entry<E>> stream() {
        return this.counter.object2IntEntrySet().stream();
    }

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
}
