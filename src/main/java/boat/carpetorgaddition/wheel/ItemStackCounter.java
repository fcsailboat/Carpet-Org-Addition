package boat.carpetorgaddition.wheel;

import boat.carpetorgaddition.util.InventoryUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ItemStackCounter implements Counter<ItemStack> {
    private final Counter<ItemStackWrapper> counter = new SimpleCounter<>();

    public ItemStackCounter() {
    }

    public void add(ItemStack itemStack) {
        this.add(itemStack, itemStack.getCount());
    }

    @Override
    public void add(ItemStack itemStack, int count) {
        this.counter.add(new ItemStackWrapper(itemStack), count);
    }

    @Override
    public void set(ItemStack itemStack, int count) {
        this.counter.set(new ItemStackWrapper(itemStack), count);
    }

    @Override
    public int getCount(ItemStack itemStack) {
        return this.counter.getCount(new ItemStackWrapper(itemStack));
    }

    @Override
    public int size() {
        return this.counter.size();
    }

    @Override
    public boolean isEmpty() {
        return this.counter.isEmpty();
    }

    @Override
    public Stream<Object2IntMap.Entry<ItemStack>> stream() {
        return this.counter.stream().map(entry -> Object2IntMap.entry(entry.getKey().itemStack(), entry.getIntValue()));
    }

    @Override
    public Set<ItemStack> keySet() {
        return this.counter.keySet().stream().map(ItemStackWrapper::itemStack).collect(Collectors.toSet());
    }

    @Override
    public Set<Object2IntMap.Entry<ItemStack>> entrySet() {
        return this.counter.stream().map(entry -> Object2IntMap.entry(entry.getKey().itemStack(), entry.getIntValue())).collect(Collectors.toSet());
    }

    @Override
    public @NonNull Iterator<ItemStack> iterator() {
        return this.counter.stream().map(Object2IntMap.Entry::getKey).map(ItemStackWrapper::itemStack).toList().iterator();
    }

    public record ItemStackWrapper(ItemStack itemStack) {
        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ItemStackWrapper wrapper = (ItemStackWrapper) o;
            return InventoryUtils.canMerge(this.itemStack, wrapper.itemStack);
        }

        @Override
        public int hashCode() {
            return ItemStack.hashItemAndComponents(itemStack);
        }
    }
}
