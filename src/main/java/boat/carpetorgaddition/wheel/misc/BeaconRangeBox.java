package boat.carpetorgaddition.wheel.misc;

import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.util.MathUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.provider.CommandProvider;
import net.minecraft.server.level.ServerPlayer;
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
            double average = MathUtils.average(maxX, minX);
            minX = Math.floor(average);
            maxX = Math.ceil(average);
        }
        if (minZ > maxZ) {
            double average = MathUtils.average(maxZ, minZ);
            minZ = Math.floor(average);
            maxZ = Math.ceil(average);
        }
        return new BeaconRangeBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * 使用粒子效果显示信标范围框
     *
     * @param player 执行命令的玩家
     * @author 文心一言
     */
    @Deprecated(forRemoval = true)
    public void show(ServerPlayer player) {
        // 定义顶点
        double[] vertices = {
                minX, minY, minZ,
                maxX, minY, minZ,
                maxX, maxY, minZ,
                minX, maxY, minZ,
                minX, minY, maxZ,
                maxX, minY, maxZ,
                maxX, maxY, maxZ,
                minX, maxY, maxZ
        };
        // 定义棱的连接顺序（只列出必要的连接）
        int[][] edges = {
                {0, 1}, {1, 2}, {2, 3}, {3, 0}, // 底部矩形
                {4, 5}, {5, 6}, {6, 7}, {7, 4}, // 顶部矩形
                {0, 4}, {1, 5}, {2, 6}, {3, 7}  // 连接底部和顶部
        };
        // 循环遍历所有棱，生成并执行命令
        for (int[] edge : edges) {
            int v1Index = edge[0] * 3;
            int v2Index = edge[1] * 3;
            // 提取顶点坐标
            double x1 = vertices[v1Index];
            double y1 = vertices[v1Index + 1];
            double z1 = vertices[v1Index + 2];
            double x2 = vertices[v2Index];
            double y2 = vertices[v2Index + 1];
            double z2 = vertices[v2Index + 2];
            // 执行命令
            CommandUtils.execute(player, CommandProvider.drawParticleLine(x1, y1, z1, x2, y2, z2));
        }
    }

    // 将信标范围调整为整个世界高度
    public BeaconRangeBox worldHeight(Level world) {
        int bottom = ServerUtils.getMinArchitectureAltitude(world) - 64;
        int top = ServerUtils.getMaxArchitectureAltitude(world) + 64;
        return new BeaconRangeBox(minX, bottom, minZ, maxX, top, maxZ);
    }
}
