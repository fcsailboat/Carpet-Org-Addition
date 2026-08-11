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

    /**
     * 类对象是不可变的，因此不需要考虑并发修改的问题
     */
    @NonNull
    @Override
    public Iterator<BlockPos> iterator() {
        return new Iterator<>() {
            /**
             * 当前迭代次数
             */
            private long iterations = 0;
            /**
             * 最大迭代次数
             */
            private final long maxIterations = BlockPosTraverser.this.size();
            private final int startX = minX;
            private final int startY = minY;
            private final int startZ = minZ;
            private final int finalX = maxX;
            private final int finalY = maxY;
            // 迭代器当前遍历到的位置
            private int currentX = startX;
            private int currentY = startY;
            private int currentZ = startZ;

            @Override
            public boolean hasNext() {
                // 当前方块坐标是否在选区内
                return this.iterations < maxIterations;
            }

            @Override
            public BlockPos next() {
                if (!hasNext()) {
                    // 超出选区抛出异常
                    throw new NoSuchElementException();
                }
                BlockPos blockPos = new BlockPos(this.currentX, this.currentY, this.currentZ);
                this.iterations++;
                this.currentX++;
                // X轴遍历到了最后，X重置，Y递增，Z轴不变
                if (this.currentX > this.finalX) {
                    this.currentX = this.startX;
                    this.currentY++;
                    if (this.currentY > this.finalY) {
                        this.currentY = this.startY;
                        this.currentZ++;
                    }
                }
                return blockPos;
            }
        };
    }
}
