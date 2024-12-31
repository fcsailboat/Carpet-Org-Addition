package org.carpetorgaddition.util.wheel;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 用来获取指定范围内所有方块坐标对象，方块坐标对象不是使用集合一次性返回的，
 * 而是使用迭代器逐个返回，因此它不会大量占用内存，并且本类实现了{@link Iterable}接口，可以使用增强for循环遍历
 */
public class SelectionArea implements Iterable<BlockPos> {
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;

    public SelectionArea(World world, BlockPos sourcePos, int range) {
        this.minX = sourcePos.getX() - Math.abs(range);
        this.minY = world.getBottomY();
        this.minZ = sourcePos.getZ() - Math.abs(range);
        this.maxX = sourcePos.getX() + Math.abs(range);
        this.maxY = world.getTopY();
        this.maxZ = sourcePos.getZ() + Math.abs(range);
    }

    public SelectionArea(BlockPos from, BlockPos to) {
        this.minX = Math.min(from.getX(), to.getX());
        this.minY = Math.min(from.getY(), to.getY());
        this.minZ = Math.min(from.getZ(), to.getZ());
        this.maxX = Math.max(from.getX(), to.getX());
        this.maxY = Math.max(from.getY(), to.getY());
        this.maxZ = Math.max(from.getZ(), to.getZ());
    }

    public SelectionArea(Box box) {
        this.minX = (int) box.minX;
        this.minY = (int) box.minY;
        this.minZ = (int) box.minZ;
        this.maxX = (int) box.maxX;
        this.maxY = (int) box.maxY;
        this.maxZ = (int) box.maxZ;
    }

    /**
     * @return 选区内方块的总数
     */
    public int size() {
        return (this.maxX - this.minX + 1) * (this.maxY - this.minY + 1) * (this.maxZ - this.minZ + 1);
    }

    /**
     * @return 与当前对象等效的Box对象
     */
    public Box toBox() {
        return new Box(this.minX, this.minY, this.minZ, this.maxX + 1, this.maxY + 1, this.maxZ + 1);
    }

    /**
     * 类对象是不可变的，因此不需要考虑并发修改的问题
     */
    @NotNull
    @Override
    public Iterator<BlockPos> iterator() {
        return new Iterator<>() {
            // 迭代器当前遍历到的位置
            private BlockPos currentPos = new BlockPos(SelectionArea.this.minX, SelectionArea.this.minY, SelectionArea.this.minZ);

            @Override
            public boolean hasNext() {
                // 当前方块坐标是否在选区内
                return currentPos.getX() <= SelectionArea.this.maxX
                        && currentPos.getY() <= SelectionArea.this.maxY
                        && currentPos.getZ() <= SelectionArea.this.maxZ;
            }

            @Override
            public BlockPos next() {
                if (!hasNext()) {
                    // 超出选区抛出异常
                    throw new NoSuchElementException();
                }
                // 当前遍历到的位置坐标的副本
                BlockPos blockPos = this.currentPos;
                this.currentPos = new BlockPos(this.currentPos.getX() + 1, this.currentPos.getY(), this.currentPos.getZ());
                // X轴遍历到了最后，X重置，Y增加1，Z轴不变
                if (this.currentPos.getX() > SelectionArea.this.maxX) {
                    this.currentPos = new BlockPos(SelectionArea.this.minX, this.currentPos.getY() + 1, this.currentPos.getZ());
                    if (this.currentPos.getY() > SelectionArea.this.maxY) {
                        this.currentPos = new BlockPos(SelectionArea.this.minX, SelectionArea.this.minY, this.currentPos.getZ() + 1);
                        if (this.currentPos.getZ() > SelectionArea.this.maxZ) {
                            // Z轴也遍历到了最后，直接将修改之前的坐标返回
                            return blockPos;
                        }
                    }
                }
                return blockPos;
            }
        };
    }
}
