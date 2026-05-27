package boat.carpetorgaddition.wheel.traverser;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class EntityTraverser<T extends Entity> extends WorldTraverser<Optional<T>> {
    private final Level world;
    private final Class<T> type;

    public EntityTraverser(Level world, BlockPos from, BlockPos to, Class<T> type) {
        super(from, to);
        this.world = world;
        this.type = type;
    }

    private List<T> entities(Class<T> type) {
        return this.world.getEntitiesOfClass(type, this.toBox());
    }

    public boolean isEmpty() {
        return !this.isPresent();
    }

    public boolean isPresent() {
        return this.iterator().hasNext();
    }

    @Override
    public @NonNull Iterator<Optional<T>> iterator() {
        return new Itr<>(this.entities(this.type));
    }

    public static class Itr<T extends Entity> implements Iterator<Optional<T>> {
        private final Iterator<T> iterator;

        public Itr(List<T> list) {
            this.iterator = list.iterator();
        }

        @Override
        public boolean hasNext() {
            return this.iterator.hasNext();
        }

        @Override
        public Optional<T> next() {
            T next = this.iterator.next();
            if (next.isRemoved()) {
                return Optional.empty();
            }
            return Optional.of(next);
        }
    }
}
