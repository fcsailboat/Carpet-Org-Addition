package boat.carpetorgaddition.wheel;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ItemStackCounter implements Counter<ItemStack> {
    private final Counter<ItemIdentity> counter = new SimpleCounter<>();

    public ItemStackCounter() {
    }

    public void add(ItemStack itemStack) {
        this.add(itemStack, itemStack.getCount());
    }

    @Override
    public void add(ItemStack itemStack, int count) {
        this.counter.add(new ItemIdentity(itemStack), count);
    }

    @Override
    public void set(ItemStack itemStack, int count) {
        this.counter.set(new ItemIdentity(itemStack), count);
    }

    @Override
    public int getCount(ItemStack itemStack) {
        return this.counter.getCount(new ItemIdentity(itemStack));
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
        return this.counter.stream().map(entry -> Object2IntMap.entry(entry.getKey().asItemStack(), entry.getIntValue()));
    }

    @Override
    public Set<ItemStack> keySet() {
        return this.counter.keySet().stream().map(ItemIdentity::asItemStack).collect(Collectors.toSet());
    }

    @Override
    public Set<Object2IntMap.Entry<ItemStack>> entrySet() {
        return this.counter.stream().map(entry -> Object2IntMap.entry(entry.getKey().asItemStack(), entry.getIntValue())).collect(Collectors.toSet());
    }

    @Override
    public @NonNull Iterator<ItemStack> iterator() {
        return this.counter.stream().map(Object2IntMap.Entry::getKey).map(ItemIdentity::asItemStack).iterator();
    }
}
