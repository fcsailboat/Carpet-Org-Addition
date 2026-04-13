package boat.carpetorgaddition.wheel.traverser;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.NonNull;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;

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
    public @NonNull Iterator<BlockEntity> iterator() {
        return new Itr();
    }

    private class Itr implements Iterator<BlockEntity> {
        private BlockEntity next;
        private Iterator<BlockEntity> current;
        private boolean alreadyNext = false;
        private final Iterator<Optional<ChunkAccess>> chunkIterator;

        private Itr() {
            ChunkTraverser traverser = new ChunkTraverser(world, from, to);
            this.chunkIterator = traverser.iterator();
        }

        @Override
        public boolean hasNext() {
            if (this.alreadyNext) {
                return this.next != null;
            }
            this.alreadyNext = true;
            // this.current != null用来在this.chunkIterator.hasNext()耗尽时处理未完成的迭代器
            while (this.current != null || this.chunkIterator.hasNext()) {
                if (this.current == null) {
                    Optional<LevelChunk> optional = this.chunkIterator.next()
                            .filter(chunk -> chunk instanceof LevelChunk)
                            .map(chunk -> (LevelChunk) chunk);
                    // 区块可能未加载
                    if (optional.isEmpty()) {
                        continue;
                    }
                    this.current = optional.get().getBlockEntities().values().iterator();
                }
                while (this.current.hasNext()) {
                    BlockEntity blockEntity = this.current.next();
                    if (BlockEntityTraverser.this.contains(blockEntity.getBlockPos())) {
                        this.next = blockEntity;
                        return true;
                    }
                }
                this.current = null;
            }
            return false;
        }

        @Override
        public BlockEntity next() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            }
            this.alreadyNext = false;
            BlockEntity blockEntity = this.next;
            this.next = null;
            return blockEntity;
        }
    }
}
