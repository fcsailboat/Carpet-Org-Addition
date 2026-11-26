package boat.carpetorgaddition.wheel.misc;

import boat.carpetorgaddition.util.WorldUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class BeaconRangeBox extends AABB {
    public BeaconRangeBox(AABB box) {
        super(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    public BeaconRangeBox(double x1, double y1, double z1, double x2, double y2, double z2) {
        super(x1, y1, z1, x2, y2, z2);
    }

    // 扩大或减小信标范围，取决于参数是正数还是负数
    public BeaconRangeBox modify(int range) {
        double minX = this.minX - range;
        double minY = this.minY;
        double minZ = this.minZ - range;
        double maxX = this.maxX + range;
        double maxY = this.maxY;
        double maxZ = this.maxZ + range;
        // 限制信标的最小范围为1x1格
        if (minX > maxX) {
            double average = (maxX + minX) / 2;
            minX = Math.floor(average);
            maxX = Math.ceil(average);
        }
        if (minZ > maxZ) {
            double average = (maxZ + minZ) / 2;
            minZ = Math.floor(average);
            maxZ = Math.ceil(average);
        }
        return new BeaconRangeBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    // 将信标范围调整为整个世界高度
    public BeaconRangeBox worldHeight(Level world) {
        int bottom = WorldUtils.getMinArchitectureAltitude(world) - 64;
        int top = WorldUtils.getMaxArchitectureAltitude(world) + 64;
        return new BeaconRangeBox(minX, bottom, minZ, maxX, top, maxZ);
    }
}
