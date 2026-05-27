package boat.carpetorgaddition.wheel.traverser;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;

public class ChunkTraverser extends WorldTraverser<Optional<ChunkAccess>> {
    private final Level world;

    public ChunkTraverser(Level world, BlockPos from, BlockPos to) {
        super(from, to);
        this.world = world;
    }

    @Override
    public @NonNull Iterator<Optional<ChunkAccess>> iterator() {
        return new Itr(this.world, this.from, this.to);
    }

    public static class Itr implements Iterator<Optional<ChunkAccess>> {
        private final ChunkPos start;
        private final ChunkPos end;
        @NonNull
        private final Level world;
        private int currentX;
        private int currentZ;

        private Itr(Level world, BlockPos from, BlockPos to) {
            this.world = world;
            LevelChunk minChunk = world.getChunkAt(from);
            LevelChunk maxChunk = world.getChunkAt(to);
            ChunkPos minChunkPos = minChunk.getPos();
            ChunkPos maxChunkPos = maxChunk.getPos();
            this.start = minChunkPos;
            this.end = maxChunkPos;
            this.currentX = minChunkPos.x();
            this.currentZ = minChunkPos.z();
        }

        @Override
        public boolean hasNext() {
            return this.currentZ <= end.z();
        }

        @Nullable
        @Override
        public Optional<ChunkAccess> next() {
            if (this.hasNext()) {
                ChunkAccess chunk = this.world.getChunk(currentX, currentZ, ChunkStatus.FULL, false);
                this.currentX++;
                if (currentX > end.x()) {
                    this.currentX = start.x();
                    this.currentZ++;
                }
                return Optional.ofNullable(chunk);
            } else {
                throw new NoSuchElementException();
            }
        }
    }
}
