package boat.carpetorgaddition.wheel;

import net.minecraft.core.BlockPos;

public final class HorizontalBlockPos {
    private final int x;
    private final int z;

    public HorizontalBlockPos(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public HorizontalBlockPos(long pos) {
        this.x = (int) (pos & 0xFFFFFFFFL);
        this.z = (int) (pos >>> 32 & 0XFFFFFFFFL);
    }

    public BlockPos toBlockPos(int y) {
        return new BlockPos(this.x, y, this.z);
    }

    public long toLong() {
        return this.x & 0xFFFFFFFFL | (this.z & 0xFFFFFFFFL) << 32;
    }
}
