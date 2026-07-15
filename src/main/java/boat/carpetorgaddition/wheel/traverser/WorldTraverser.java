package boat.carpetorgaddition.wheel.traverser;

import boat.carpetorgaddition.util.MathUtils;
import boat.carpetorgaddition.util.ServerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.Objects;

public abstract class WorldTraverser<T> implements Iterable<T> {
    protected final int minX;
    protected final int minY;
    protected final int minZ;
    protected final int maxX;
    protected final int maxY;
    protected final int maxZ;
    protected final BlockPos from;
    protected final BlockPos to;

    public WorldTraverser(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.minX = Math.max(minX, -Level.MAX_LEVEL_SIZE);
        this.minY = Math.max(minY, Level.MIN_ENTITY_SPAWN_Y);
        this.minZ = Math.max(minZ, -Level.MAX_LEVEL_SIZE);
        this.maxX = Math.min(maxX, Level.MAX_LEVEL_SIZE);
        this.maxY = Math.min(maxY, Level.MAX_ENTITY_SPAWN_Y);
        this.maxZ = Math.min(maxZ, Level.MAX_LEVEL_SIZE);
        this.from = new BlockPos(this.minX, this.minY, this.minZ);
        this.to = new BlockPos(this.maxX, this.maxY, this.maxZ);
    }

    public WorldTraverser(Level world, BlockPos sourcePos, int range) {
        this(
                sourcePos.getX() - range,
                ServerUtils.getMinArchitectureAltitude(world),
                sourcePos.getZ() - range,
                sourcePos.getX() + range,
                ServerUtils.getMaxArchitectureAltitude(world),
                sourcePos.getZ() + range
        );
    }

    public WorldTraverser(BlockPos from, BlockPos to) {
        this(
                Math.min(from.getX(), to.getX()),
                Math.min(from.getY(), to.getY()),
                Math.min(from.getZ(), to.getZ()),
                Math.max(from.getX(), to.getX()),
                Math.max(from.getY(), to.getY()),
                Math.max(from.getZ(), to.getZ())
        );
    }

    public WorldTraverser(AABB box) {
        this((int) box.minX, (int) box.minY, (int) box.minZ, (int) box.maxX, (int) box.maxY, (int) box.maxZ);
    }

    /**
     * @return 选区内方块的总数
     */
    public long size() {
        return (long) this.length() * this.width() * this.height();
    }

    /**
     * @return 选区的长度
     */
    public int length() {
        return Math.max(this.maxX - this.minX + 1, 0);
    }

    /**
     * @return 选区的高度
     */
    public int height() {
        return Math.max(this.maxY - this.minY + 1, 0);
    }

    /**
     * @return 选区的宽度
     */
    public int width() {
        return Math.max(this.maxZ - this.minZ + 1, 0);
    }

    /**
     * @return 与当前对象等效的Box对象
     */
    public AABB toBox() {
        return new AABB(this.minX, this.minY, this.minZ, this.maxX + 1, this.maxY + 1, this.maxZ + 1);
    }

    /**
     * @return 该选区是否包含指定位置
     */
    public boolean contains(BlockPos blockPos) {
        int x = blockPos.getX();
        int y = blockPos.getY();
        int z = blockPos.getZ();
        return x >= this.minX && x <= this.maxX && y >= this.minY && y <= this.maxY && z >= this.minZ && z <= this.maxZ;
    }

    /**
     * @return 随机获取一个方块坐标
     */
    public BlockPos randomBlockPos() {
        int x = MathUtils.randomInt(this.minX, this.maxX);
        int y = MathUtils.randomInt(this.minY, this.maxY);
        int z = MathUtils.randomInt(this.minZ, this.maxZ);
        return new BlockPos(x, y, z);
    }

    /**
     * @return XYZ轴正方向最大的方块坐标
     */
    public BlockPos getMaxBlockPos() {
        return this.to;
    }

    /**
     * @return XYZ轴正方向最小的方块坐标
     */
    public BlockPos getMinBlockPos() {
        return this.from;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WorldTraverser<?> that = (WorldTraverser<?>) o;
        return minX == that.minX
               && minY == that.minY
               && minZ == that.minZ
               && maxX == that.maxX
               && maxY == that.maxY
               && maxZ == that.maxZ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
