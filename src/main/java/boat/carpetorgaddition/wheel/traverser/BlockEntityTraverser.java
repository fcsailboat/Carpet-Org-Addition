package boat.carpetorgaddition.wheel.traverser;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class BlockEntityTraverser extends WorldTraverser<BlockEntity> {
    private final Level world;

    public BlockEntityTraverser(Level world, BlockPos sourcePos, int range) {
        super(world, sourcePos, range);
        this.world = world;
    }

    public BlockEntityTraverser(Level world, BlockPos from, BlockPos to) {
        super(from, to);
        this.world = world;
    }

    @Override
    public @NonNull TraverseCursor<BlockEntity> getCursor() {
        return new Cursor(this.world, this.from, this.to);
    }

    private class Cursor implements TraverseCursor<BlockEntity> {
        private BlockEntity next;
        private Iterator<BlockEntity> current;
        private LevelChunk currentChunk;
        private int currentChunkBlockEntityTotal;
        private int currentChunkBlockEntityVisitedCount;
        /// 已经遍历过的方块总数，包含方块实体，也包含普通方块
        private long processedCount = 0L;
        private final Iterator<ChunkAccess> chunkIterator;

        private Cursor(Level world, BlockPos from, BlockPos to) {
            ChunkTraverser traverser = new ChunkTraverser(world, from, to);
            this.chunkIterator = traverser.iterator();
        }

        @Override
        public Step advance(long timeSliceMillis) {
            if (this.next != null) {
                return Step.ELEMENT;
            }
            long start = System.currentTimeMillis();
            while (this.current != null || this.chunkIterator.hasNext()) {
                if (System.currentTimeMillis() - start > timeSliceMillis) {
                    return Step.PAUSED;
                }
                if (this.current == null) {
                    ChunkAccess chunkAccess = this.chunkIterator.next();
                    if (chunkAccess instanceof LevelChunk chunk) {
                        Map<BlockPos, BlockEntity> entities = chunk.getBlockEntities();
                        if (entities.isEmpty()) {
                            this.addChunkToProcessedBlockCount(chunk);
                            continue;
                        }
                        Collection<BlockEntity> blockEntities;
                        if (contains(chunk.getPos()) && isCompleteHeight(world)) {
                            blockEntities = entities.values();
                        } else if (intersects(chunk.getPos())) {
                            blockEntities = entities.entrySet()
                                    .stream()
                                    .filter(entry -> contains(entry.getKey()))
                                    .map(Map.Entry::getValue)
                                    .toList();
                            if (blockEntities.isEmpty()) {
                                this.addChunkToProcessedBlockCount(chunk);
                                continue;
                            }
                        } else {
                            this.addChunkToProcessedBlockCount(chunk);
                            continue;
                        }
                        this.current = blockEntities.iterator();
                        this.currentChunkBlockEntityTotal = blockEntities.size();
                        this.currentChunk = chunk;
                    } else {
                        this.addChunkToProcessedBlockCount(chunkAccess);
                        continue;
                    }
                }
                if (this.current.hasNext()) {
                    BlockEntity blockEntity = this.current.next();
                    this.currentChunkBlockEntityVisitedCount++;
                    this.next = blockEntity;
                    return Step.ELEMENT;
                }
                this.current = null;
                this.addChunkToProcessedBlockCount(this.getProcessedCount(this.currentChunk));
                this.currentChunk = null;
                this.currentChunkBlockEntityTotal = 0;
                this.currentChunkBlockEntityVisitedCount = 0;
            }
            return Step.DONE;
        }

        private void addChunkToProcessedBlockCount(@NonNull ChunkAccess chunk) {
            this.addChunkToProcessedBlockCount((long) getOverlapBlockCount(chunk.getPos()) * height());
        }

        private void addChunkToProcessedBlockCount(long count) {
            this.processedCount += count;
        }

        @Override
        public @NonNull BlockEntity take() {
            BlockEntity blockEntity = this.next;
            this.next = null;
            return blockEntity;
        }

        private long getProcessedCount(@Nullable ChunkAccess chunk) {
            if (chunk == null || this.currentChunkBlockEntityTotal == 0L) {
                return 0L;
            }
            long chunkBlockTotal = (long) getOverlapBlockCount(chunk.getPos()) * height();
            return Math.round(((double) this.currentChunkBlockEntityVisitedCount / this.currentChunkBlockEntityTotal) * chunkBlockTotal);
        }

        @Override
        public long getProcessedCount() {
            return this.processedCount + this.getProcessedCount(this.currentChunk);
        }
    }
}
