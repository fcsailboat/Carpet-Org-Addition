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

public class QuickBlockPosTraverser extends WorldTraverser<Optional<BlockPos>> {
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
    public QuickBlockPosTraverser.@NonNull QuickBlockPosIterator iterator() {
        return new QuickBlockPosIterator(this.world, this.from, this.to);
    }

    public class QuickBlockPosIterator implements Iterator<Optional<BlockPos>> {
        private final Iterator<Optional<ChunkAccess>> iterator;
        /**
         * 已经遍历过的方块数量，包含空气
         */
        private long count = 0L;
        private boolean timeout = false;
        private ChunkAccess currentChunk;
        private BlockPos next = null;
        private int sectionIndex;
        private LevelChunkSection[] sections;
        private int x;
        private int y;
        private int z;

        private QuickBlockPosIterator(Level world, BlockPos from, BlockPos to) {
            ChunkTraverser traverser = new ChunkTraverser(world, from, to);
            this.iterator = traverser.iterator();
        }

        @Override
        public boolean hasNext() {
            if (this.next != null) {
                return true;
            }
            this.timeout = false;
            long l = System.currentTimeMillis();
            while (true) {
                if (System.currentTimeMillis() - l >= 20) {
                    this.timeout = true;
                    return true;
                }
                if (this.sections == null) {
                    while (this.iterator.hasNext()) {
                        Optional<ChunkAccess> optional = this.iterator.next();
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
                    return false;
                }
                BlockPos blockPos = this.findNoAirBlockPos(this.sections);
                if (blockPos == null) {
                    this.sections = null;
                    continue;
                }
                this.next = blockPos;
                return true;
            }
        }

        private BlockPos findNoAirBlockPos(LevelChunkSection[] sections) {
            while (this.sectionIndex < sections.length) {
                LevelChunkSection section = sections[this.sectionIndex];
                ChunkPos chunkPos = this.currentChunk.getPos();
                int minBuildHeight = ServerUtils.getMinArchitectureAltitude(world);
                int sectionMinY = minBuildHeight + this.sectionIndex * 16;
                int sectionMaxY = sectionMinY + 15;
                int height = getOverlapHeight(sectionMinY, sectionMaxY);
                if (section.hasOnlyAir() || height == 0 || !(paletteMatcher != null && section.maybeHas(paletteMatcher))) {
                    this.sectionIndex++;
                    if (contains(chunkPos)) {
                        this.count += 16L * height * 16L;
                    } else if (intersects(chunkPos)) {
                        this.count += getOverlapBlockCount(chunkPos) * (long) height;
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
                    this.count++;
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
        public Optional<BlockPos> next() {
            if (this.timeout) {
                return Optional.empty();
            }
            if (this.next == null) {
                throw new NoSuchElementException();
            }
            BlockPos result = this.next;
            this.next = null;
            return Optional.of(result);
        }

        public long getCount() {
            return this.count;
        }
    }
}
