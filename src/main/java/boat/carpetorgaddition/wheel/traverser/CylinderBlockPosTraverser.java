package boat.carpetorgaddition.wheel.traverser;

import boat.carpetorgaddition.util.MathUtils;
import it.unimi.dsi.fastutil.ints.Int2LongArrayMap;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.NonNull;

import java.util.Iterator;
import java.util.Objects;

public class CylinderBlockPosTraverser extends BlockPosTraverser {
    private final BlockPos center;
    private final int radius;
    private long size = -1;
    private static final Int2LongMap SIZE_CACHE = new Int2LongArrayMap();

    public CylinderBlockPosTraverser(BlockPos center, int radius, int height) {
        super(
                center.getX() - radius,
                center.getY(),
                center.getZ() - radius,
                center.getX() + radius,
                center.getY() + height - 1,
                center.getZ() + radius
        );
        this.center = center;
        this.radius = radius;
    }

    @Override
    public boolean contains(BlockPos blockPos) {
        return super.contains(blockPos) && MathUtils.getCalculateBlockIntegerDistance(this.center, blockPos) <= this.radius;
    }

    @Override
    public long size() {
        if (this.size == -1L) {
            if (super.size() == 0L) {
                this.size = 0L;
            } else {
                if (SIZE_CACHE.containsKey(this.radius)) {
                    this.size = SIZE_CACHE.get(this.radius) * this.height();
                } else {
                    // 最大最小高度相同，即高度为1的区域
                    BlockPosTraverser traverser = new BlockPosTraverser(this.minX, this.minY, this.minZ, this.maxX, this.minY, this.maxZ);
                    long size = 0L;
                    for (BlockPos blockPos : traverser) {
                        if (this.contains(blockPos)) {
                            size++;
                        }
                    }
                    SIZE_CACHE.put(this.radius, size);
                    this.size = size * this.height();
                }
            }
        }
        return this.size;
    }

    @Override
    public BlockPos randomBlockPos() {
        while (true) {
            BlockPos blockPos = super.randomBlockPos();
            if (this.contains(blockPos)) {
                return blockPos;
            }
        }
    }

    public BlockPos getCenter() {
        return center;
    }

    public int getRadius() {
        return radius;
    }

    public int getHeight() {
        return this.height();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CylinderBlockPosTraverser blockPos = (CylinderBlockPosTraverser) o;
        return radius == blockPos.radius && Objects.equals(center, blockPos.center);
    }

    @Override
    public int hashCode() {
        return Objects.hash(center, radius);
    }

    @Override
    @NonNull
    public Iterator<BlockPos> iterator() {
        return new Iterator<>() {
            private final Iterator<BlockPos> iterator = CylinderBlockPosTraverser.super.iterator();
            private BlockPos next;

            @Override
            public boolean hasNext() {
                if (this.next == null) {
                    while (this.iterator.hasNext()) {
                        BlockPos next = this.iterator.next();
                        if (contains(next)) {
                            this.next = next;
                            return true;
                        }
                    }
                    return false;
                }
                return true;
            }

            @Override
            public BlockPos next() {
                BlockPos result = this.next;
                this.next = null;
                return result;
            }
        };
    }
}
