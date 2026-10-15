package boat.carpetorgaddition.wheel.traverser;

import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.HorizontalBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.NonNull;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 用来获取指定范围内所有方块坐标对象，方块坐标对象不是使用集合一次性返回的，
 * 而是使用迭代器逐个返回，因此它不会大量占用内存，并且本类实现了{@link Iterable}接口，可以使用增强for循环遍历
 */
public class BlockPosTraverser extends WorldTraverser<BlockPos> {
    protected BlockPosTraverser(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        super(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public BlockPosTraverser(BlockPos blockPos, int range) {
        this(
                blockPos.getX() - range,
                blockPos.getY() - range,
                blockPos.getZ() - range,
                blockPos.getX() + range,
                blockPos.getY() + range,
                blockPos.getZ() + range
        );
    }

    public BlockPosTraverser(Level world, BlockPos sourcePos, int range) {
        super(world, sourcePos, range);
    }

    public BlockPosTraverser(BlockPos from, BlockPos to) {
        super(from, to);
    }

    public BlockPosTraverser(AABB box) {
        super(box);
    }

    @SuppressWarnings("unused")
    public BlockPosTraverser clamp(Level world) {
        int minY = Math.max(this.minY, ServerUtils.getMinArchitectureAltitude(world));
        int maxY = Math.min(this.maxY, ServerUtils.getMaxArchitectureAltitude(world));
        return new BlockPosTraverser(this.minX, minY, this.minZ, this.maxX, maxY, this.maxZ);
    }

    public int getMinY() {
        return this.minY;
    }

    public int getMaxY() {
        return this.maxY;
    }

    public Iterable<HorizontalBlockPos> horizontalBlockPositions() {
        return new Iterable<>() {
            @Override
            public @NonNull Iterator<HorizontalBlockPos> iterator() {
                return new Iterator<>() {
                    private int currentX = minX;
                    private int currentZ = minZ;

                    @Override
                    public boolean hasNext() {
                        return this.currentX <= maxX && this.currentZ <= maxZ;
                    }

                    @Override
                    public HorizontalBlockPos next() {
                        if (!this.hasNext()) {
                            throw new NoSuchElementException();
                        }
                        HorizontalBlockPos horizontalBlockPos = new HorizontalBlockPos(this.currentX, this.currentZ);
                        this.currentX++;
                        if (this.currentX > maxX) {
                            this.currentX = minX;
                            this.currentZ++;
                        }
                        return horizontalBlockPos;
                    }
                };
            }
        };
    }

    @Override
    public @NonNull TraverseCursor<BlockPos> getCursor() {
        return new Cursor(this);
    }

    private static class Cursor implements TraverseCursor<BlockPos> {
        /**
         * 当前迭代次数
         */
        private long processedCount = 0;
        /**
         * 最大迭代次数
         */
        private final long totalCount;
        private final int startX;
        private final int startY;
        private final int endX;
        private final int endY;
        private int nextX;
        private int nextY;
        private int nextZ;

        public Cursor(BlockPosTraverser traverser) {
            this.totalCount = traverser.size();
            this.startX = traverser.minX;
            this.startY = traverser.minY;
            this.endX = traverser.maxX;
            this.endY = traverser.maxY;
            this.nextX = this.startX;
            this.nextY = this.startY;
            this.nextZ = traverser.minZ;
        }

        @Override
        public Step advance(long timeSlice) {
            // 当前方块坐标是否在选区内
            return this.processedCount < totalCount ? Step.ELEMENT : Step.DONE;
        }

        @Override
        public BlockPos take() {
            if (this.processedCount >= this.totalCount) {
                // 超出选区抛出异常
                throw new NoSuchElementException();
            }
            BlockPos blockPos = new BlockPos(this.nextX, this.nextY, this.nextZ);
            this.processedCount++;
            this.nextX++;
            // X轴遍历到了最后，X重置，Y递增，Z轴不变
            if (this.nextX > this.endX) {
                this.nextX = this.startX;
                this.nextY++;
                if (this.nextY > this.endY) {
                    this.nextY = this.startY;
                    this.nextZ++;
                }
            }
            return blockPos;
        }

        @Override
        public long getProcessedCount() {
            return this.processedCount;
        }
    }
}
