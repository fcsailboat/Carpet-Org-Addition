package boat.carpetorgaddition.wheel.traverser;

import boat.carpetorgaddition.util.ServerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Predicate;

public class QuickBlockPosTraverser extends WorldTraverser<BlockPos> {
    private final Level world;
    @Nullable
    private final Predicate<BlockState> paletteMatcher;

    public QuickBlockPosTraverser(Level world, BlockPos from, BlockPos to, @Nullable Predicate<BlockState> paletteMatcher) {
        super(from, to);
        this.world = world;
        this.paletteMatcher = paletteMatcher;
    }

    public QuickBlockPosTraverser(Level world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, @Nullable Predicate<BlockState> paletteMatcher) {
        super(minX, minY, minZ, maxX, maxY, maxZ);
        this.world = world;
        this.paletteMatcher = paletteMatcher;
    }

    public QuickBlockPosTraverser(ServerLevel world, BlockPos sourceBlockPos, int range, @Nullable Predicate<BlockState> paletteMatcher) {
        super(world, sourceBlockPos, range);
        this.world = world;
        this.paletteMatcher = paletteMatcher;
    }

    public QuickBlockPosTraverser clamp(Level world) {
        int minY = Math.max(this.minY, ServerUtils.getMinArchitectureAltitude(world));
        int maxY = Math.min(this.maxY, ServerUtils.getMaxArchitectureAltitude(world));
        return new QuickBlockPosTraverser(world, this.minX, minY, this.minZ, this.maxX, maxY, this.maxZ, this.paletteMatcher);
    }

    @Override
    public @NonNull TraverseCursor<BlockPos> getCursor() {
        return new Cursor(this.world, this.from, this.to, this.paletteMatcher);
    }

    private class Cursor implements TraverseCursor<BlockPos> {
        @Nullable
        private final Predicate<BlockState> paletteMatcher;
        private final Iterator<Optional<ChunkAccess>> chunkIt;
        /**
         * 已经遍历过的方块数量，包含空气
         */
        private long processedCount = 0L;
        private ChunkAccess currentChunk;
        private BlockPos next = null;
        private int sectionIndex;
        private LevelChunkSection[] sections;
        private int x;
        private int y;
        private int z;

        private Cursor(Level world, BlockPos from, BlockPos to, @Nullable Predicate<BlockState> paletteMatcher) {
            ChunkTraverser traverser = new ChunkTraverser(world, from, to);
            this.paletteMatcher = paletteMatcher;
            this.chunkIt = traverser.iterator();
        }

        @Override
        public Step advance(long timeSlice) {
            if (this.next != null) {
                return Step.ELEMENT;
            }
            long l = System.currentTimeMillis();
            while (true) {
                if (System.currentTimeMillis() - l >= timeSlice) {
                    return Step.PAUSED;
                }
                if (this.sections == null) {
                    while (this.chunkIt.hasNext()) {
                        Optional<ChunkAccess> optional = this.chunkIt.next();
                        if (optional.isEmpty()) {
                            continue;
                        }
                        ChunkAccess access = optional.get();
                        this.currentChunk = access;
                        this.sections = access.getSections();
                        this.sectionIndex = 0;
                        this.resetPos();
                        break;
                    }
                }
                if (this.sections == null) {
                    return Step.DONE;
                }
                BlockPos blockPos = this.findNoAirBlockPos(this.sections);
                if (blockPos == null) {
                    this.sections = null;
                    continue;
                }
                this.next = blockPos;
                return Step.ELEMENT;
            }
        }

        @Override
        public BlockPos take() {
            if (this.next == null) {
                throw new NoSuchElementException();
            }
            BlockPos result = this.next;
            this.next = null;
            return result;
        }

        private BlockPos findNoAirBlockPos(LevelChunkSection[] sections) {
            while (this.sectionIndex < sections.length) {
                LevelChunkSection section = sections[this.sectionIndex];
                ChunkPos chunkPos = this.currentChunk.getPos();
                int minBuildHeight = ServerUtils.getMinArchitectureAltitude(world);
                int sectionMinY = minBuildHeight + this.sectionIndex * 16;
                int sectionMaxY = sectionMinY + 15;
                int height = getOverlapHeight(sectionMinY, sectionMaxY);
                if (this.shouldSkip(section, height)) {
                    this.sectionIndex++;
                    if (contains(chunkPos)) {
                        this.processedCount += 16L * height * 16L;
                    } else if (intersects(chunkPos)) {
                        this.processedCount += getOverlapBlockCount(chunkPos) * (long) height;
                    }
                    continue;
                }
                BlockPos blockPos = this.findNoAirBlockPos(section);
                if (blockPos != null) {
                    return blockPos;
                }
                this.sectionIndex++;
                this.resetPos();
            }
            return null;
        }

        private boolean shouldSkip(LevelChunkSection section, int height) {
            if (section.hasOnlyAir() || height == 0) {
                return true;
            }
            if (this.paletteMatcher == null) {
                return false;
            }
            return !section.maybeHas(this.paletteMatcher);
        }

        private void resetPos() {
            this.x = 0;
            this.y = 0;
            this.z = 0;
        }

        private BlockPos findNoAirBlockPos(LevelChunkSection section) {
            while (this.x < 16 && this.y < 16 && this.z < 16) {
                BlockState blockState = section.getBlockState(this.x, this.y, this.z);
                ChunkPos chunkPos = this.currentChunk.getPos();
                BlockPos blockPos = new BlockPos(
                        SectionPos.sectionToBlockCoord(chunkPos.x(), this.x),
                        ServerUtils.getMinArchitectureAltitude(world) + SectionPos.sectionToBlockCoord(this.sectionIndex, this.y),
                        SectionPos.sectionToBlockCoord(chunkPos.z(), this.z)
                );
                boolean contains = QuickBlockPosTraverser.this.contains(blockPos);
                if (contains) {
                    this.processedCount++;
                }
                this.x++;
                if (this.x >= 16) {
                    this.x = 0;
                    this.y++;
                    if (this.y >= 16) {
                        this.y = 0;
                        this.z++;
                    }
                }
                if (!blockState.isAir() && contains) {
                    return blockPos;
                }
            }
            this.resetPos();
            return null;
        }

        @Override
        public long getProcessedCount() {
            return this.processedCount;
        }
    }
}
