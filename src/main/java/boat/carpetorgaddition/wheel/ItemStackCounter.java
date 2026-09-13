package boat.carpetorgaddition.wheel;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.jspecify.annotations.NonNull;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ItemStackCounter implements Counter<ItemStack> {
    private final Counter<ItemIdentity> counter;

    public ItemStackCounter() {
        this(false);
    }

    public ItemStackCounter(boolean ordered) {
        this.counter = ordered ? new SimpleCounter<>(new Object2IntLinkedOpenHashMap<>()) : new SimpleCounter<>();
    }

    public void add(ItemStack itemStack) {
        this.add(itemStack, itemStack.getCount());
    }

    public void add(ItemStackTemplate template) {
        this.counter.add(new ItemIdentity(template), template.count());
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
    public Stream<Object2IntMap.Entry<ItemStack>> stream() {
        return this.counter.stream().map(entry -> Object2IntMap.entry(entry.getKey().asItemStack(), entry.getIntValue()));
    }

    @Override
    public Set<Object2IntMap.Entry<ItemStack>> entrySet() {
        return this.counter.stream()
                .map(entry -> Object2IntMap.entry(entry.getKey().asItemStack(), entry.getIntValue()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public @NonNull Iterator<ItemStack> iterator() {
        return this.counter.stream().map(Object2IntMap.Entry::getKey).map(ItemIdentity::asItemStack).iterator();
    }
}
