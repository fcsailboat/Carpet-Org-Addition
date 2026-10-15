package boat.carpetorgaddition.wheel.traverser;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jspecify.annotations.NonNull;

import java.util.NoSuchElementException;
import java.util.Optional;

public class ChunkTraverser extends WorldTraverser<Optional<ChunkAccess>> {
    private final Level world;

    public ChunkTraverser(Level world, BlockPos from, BlockPos to) {
        super(from, to);
        this.world = world;
    }

    @Override
    public @NonNull TraverseCursor<Optional<ChunkAccess>> getCursor() {
        return new Cursor(this.world, this.from, this.to);
    }

    private static class Cursor implements TraverseCursor<Optional<ChunkAccess>> {
        private final ChunkPos start;
        private final ChunkPos end;
        @NonNull
        private final Level world;
        private long processedCount = 0L;
        private int currentX;
        private int currentZ;

        private Cursor(Level world, BlockPos from, BlockPos to) {
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
        public Step advance(long timeSlice) {
            return this.currentZ <= end.z() ? Step.ELEMENT : Step.DONE;
        }

        @Override
        public Optional<ChunkAccess> take() {
            if (this.currentZ > end.z()) {
                throw new NoSuchElementException();
            }
            this.processedCount++;
            ChunkAccess chunk = this.world.getChunk(this.currentX, this.currentZ, ChunkStatus.FULL, true);
            this.currentX++;
            if (currentX > end.x()) {
                this.currentX = start.x();
                this.currentZ++;
            }
            return Optional.ofNullable(chunk);
        }

        @Override
        public long getProcessedCount() {
            return this.processedCount;
        }
    }
}
