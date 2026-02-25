package boat.carpetorgaddition.wheel;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public interface Counter<E> extends Iterable<E> {
    /**
     * 将元素数量添加指定值
     */
    void add(E element, int count);

    /**
     * 将元素数量减少指定值
     */
    default void subtract(E element, int count) {
        this.add(element, -count);
    }

    /**
     * 设置元素数量
     */
    void set(E element, int count);

    /**
     * 获取指定元素的数量
     */
    int getCount(E element);

    /**
     * 获取元素的种类数量
     */
    int size();

    /**
     * 计数器是否未记录任何元素
     */
    boolean isEmpty();

    Stream<Object2IntMap.Entry<E>> stream();

    Set<E> keySet();

    Set<Object2IntMap.Entry<E>> entrySet();

    default E getMostOrDefault(E value) {
        return this.stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(value);
    }
}
