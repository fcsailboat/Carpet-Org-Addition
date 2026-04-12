package boat.carpetorgaddition.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Range;
import org.joml.Vector3f;

import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class MathUtils {
    /**
     * 数学工具类，私有化构造方法
     */
    private MathUtils() {
    }

    private static final Random RANDOM = new Random();
    /**
     * 所有水平的基本方向
     */
    public static final Direction[] HORIZONTAL = Arrays.stream(Direction.values())
            .filter(direction -> direction.getAxis().isHorizontal())
            .toArray(Direction[]::new);

    /**
     * 获取两个方块坐标的距离的平方
     *
     * @param fromBlockPos 第一个方块坐标
     * @param toBlockPos   第二个方块坐标
     * @return 两个方块坐标距离的平方
     */
    public static double getBlockSquareDistance(BlockPos fromBlockPos, BlockPos toBlockPos) {
        // 虽然是两个整数相减，但此处的结果不能使用int接收
        // 因为整数和整数之间的运算结果只能是整数，而整数的取值范围相对有限，如果数值较大，有可能发生数值溢出
        double x = fromBlockPos.getX() - toBlockPos.getX();
        double y = fromBlockPos.getY() - toBlockPos.getY();
        double z = fromBlockPos.getZ() - toBlockPos.getZ();
        return x * x + y * y + z * z;
    }

    /**
     * 获取两个方块坐标的距离
     *
     * @param fromBlockPos 第一个方块的坐标
     * @param toBlockPos   第二个方块的坐标
     * @return 两个方块的距离
     */
    public static double getBlockDistance(BlockPos fromBlockPos, BlockPos toBlockPos) {
        return Math.sqrt(getBlockSquareDistance(fromBlockPos, toBlockPos));
    }

    /**
     * 获取两个方块坐标的整数距离(四舍五入)
     *
     * @param fromBlockPos 第一个方块的坐标
     * @param toBlockPos   第二个方块的坐标
     * @return 两个方块之间四舍五入的整数距离
     */
    public static int getBlockIntegerDistance(BlockPos fromBlockPos, BlockPos toBlockPos) {
        return (int) Math.round(Math.sqrt(getBlockSquareDistance(fromBlockPos, toBlockPos)));
    }

    /**
     * 获取两个方块水平坐标的整数距离(四舍五入)
     *
     * @param fromBlockPos 第一个方块的坐标
     * @param toBlockPos   第二个方块的坐标
     * @return 两个方块之间四舍五入的整数水平距离
     */
    public static int getCalculateBlockIntegerDistance(BlockPos fromBlockPos, BlockPos toBlockPos) {
        double x = fromBlockPos.getX() - toBlockPos.getX();
        double z = fromBlockPos.getZ() - toBlockPos.getZ();
        return (int) Math.round(Math.sqrt(x * x + z * z));
    }

    /**
     * 计算两个方块坐标的曼哈顿距离
     */
    public static int calculateManhattanDistance(BlockPos start, BlockPos end) {
        int x = start.getX() - end.getX();
        int y = start.getY() - end.getY();
        int z = start.getZ() - end.getZ();
        return Math.abs(x) + Math.abs(y) + Math.abs(z);
    }

    /**
     * 计算两个方块坐标的水平曼哈顿距离
     */
    public static int calculateHorizontalManhattanDistance(BlockPos start, BlockPos end) {
        int x = start.getX() - end.getX();
        int z = start.getZ() - end.getZ();
        return Math.abs(x) + Math.abs(z);
    }

    /**
     * 在集合中从近到远排序<br>
     *
     * @param blockPos   源方块坐标
     * @param o1BlockPos 要在集合中添加的方块坐标
     * @param o2BlockPos 集合中已有的方块坐标
     * @return 根据距离返回1或-1
     */
    public static int compareBlockPos(final BlockPos blockPos, BlockPos o1BlockPos, BlockPos o2BlockPos) {
        // 坐标1到原坐标的距离
        double distance1 = getBlockSquareDistance(blockPos, o1BlockPos);
        // 坐标2到原坐标的距离
        double distance2 = getBlockSquareDistance(blockPos, o2BlockPos);
        // 比较两个距离的大小
        return Double.compare(distance1, distance2);
    }

    /**
     * 根据在主世界的指定位置获取下界的方块位置坐标
     */
    public static BlockPos getTheNetherPos(double xPos, double yPos, double zPos) {
        return new BlockPos((int) Math.round(xPos / 8), (int) Math.round(yPos), (int) Math.round(zPos / 8));
    }

    /**
     * 根据在主世界实体的位置获取对应下界的方块位置坐标
     *
     * @param entity 一个实体，根据这个实体的位置获取对应下界的方块位置
     */
    public static BlockPos getTheNetherPos(Entity entity) {
        return getTheNetherPos(entity.getX(), entity.getY(), entity.getZ());
    }

    /**
     * 根据指定下界位置坐标获取在主世界对应的坐标
     */
    public static BlockPos getOverworldPos(double xPos, double yPos, double zPos) {
        return new BlockPos((int) Math.round(xPos * 8), (int) Math.round(yPos), (int) Math.round(zPos * 8));
    }

    /**
     * 根据实体在下界的位置坐标获取对应主世界的方块位置坐标
     *
     * @param entity 一个实体，根据这个实体的位置获取在主世界对应的坐标
     */
    public static BlockPos getOverworldPos(Entity entity) {
        return getOverworldPos(entity.getX(), entity.getY(), entity.getZ());
    }

    /**
     * 计算两个方块坐标的最小边界点
     */
    @SuppressWarnings("unused")
    public static BlockPos toMinBlockPos(BlockPos pos1, BlockPos pos2) {
        return new BlockPos(
                Math.min(pos1.getX(), pos2.getX()),
                Math.min(pos1.getY(), pos2.getY()),
                Math.min(pos1.getZ(), pos2.getZ())
        );
    }

    /**
     * 计算两个方块坐标的最大边界点
     */
    public static BlockPos toMaxBlockPos(BlockPos pos1, BlockPos pos2) {
        return new BlockPos(
                Math.max(pos1.getX(), pos2.getX()),
                Math.max(pos1.getY(), pos2.getY()),
                Math.max(pos1.getZ(), pos2.getZ())
        );
    }

    /**
     * 生成一次指定范围内随机整数，包含最大值和最小值，范围也可以包含负数，最大最小值参数也可以反过来传递
     *
     * @param min 随机数的最小值
     * @param max 随机数的最大值
     * @return 指定范围内的随机数
     */
    public static int randomInt(int min, int max) {
        if (max == min) {
            // 如果最大最小值相等，直接返回
            return max;
        }
        if (min > max) {
            // 如果最小值大于最大值，交换最大最小值
            int temp = max;
            max = min;
            min = temp;
        }
        return RANDOM.nextInt(max - min + 1) + min;
    }

    /**
     * 判断一个整数是否介于两个整数之间
     *
     * @param min    范围的最小值
     * @param max    范围的最大值
     * @param number 要检查是否介于这两个数之间的数
     */
    public static boolean isInRange(int min, int max, int number) {
        if (min > max) {
            return false;
        }
        return max >= number && number >= min;
    }

    public static boolean isInRange(double first, double second, double mid) {
        return (first >= mid && mid >= second) || (first <= mid && mid <= second);
    }

    public static boolean isInRange(Vec3 first, Vec3 second, Vec3 mid) {
        return isInRange(first.x(), second.x(), mid.x()) && isInRange(first.y(), second.y(), mid.y()) && isInRange(first.z(), second.z(), mid.z());
    }

    /**
     * @return 指定时间到当前时间相差的毫秒数
     */
    public static long timeDifference(long time) {
        return System.currentTimeMillis() - time;
    }

    /**
     * 求一个小数数组里所有数的平均值
     *
     * @param args 小数数组
     * @return 数组内所有数的平均值
     */
    public static double average(double... args) {
        double sum = 0;
        for (double arg : args) {
            sum += arg;
        }
        return sum / args.length;
    }

    /**
     * 将一个浮点数格式化为保留两位小数的字符串
     */
    @Deprecated
    public static String numberToTwoDecimalString(double number) {
        return String.format("%.2f", number);
    }

    /**
     * @param value 将一个浮点数格式化为保留最多两位小数的字符串
     */
    public static String formatToMaxTwoDecimals(double value) {
        DecimalFormat format = new DecimalFormat("#.##");
        return format.format(value);
    }

    /**
     * 让一个数逐渐趋近于另一个数
     */
    public static int approach(int start, int target) {
        if (start == target) {
            return start;
        }
        return start > target ? start - 1 : start + 1;
    }

    /**
     * 根据比例因子使一个数逐渐趋近于另一个数
     *
     * @param start  起始数值
     * @param target 目标数值
     * @param factor {@code start}趋近于{@code target}的程度，需要在0到1之间
     * @return {@code start}趋近于{@code target}后的新数值
     */
    public static double approach(double start, double target, double factor) {
        return start + (target - start) * factor;
    }

    public static Vec3 approach(Vec3 from, Vec3 to, double factor) {
        double x = approach(from.x(), to.x(), factor);
        double y = approach(from.y(), to.y(), factor);
        double z = approach(from.z(), to.z(), factor);
        return new Vec3(x, y, z);
    }

    public static Vector3f approach(Vector3f from, Vector3f to, float factor) {
        float x = (float) approach(from.x(), to.x(), factor);
        float y = (float) approach(from.y(), to.y(), factor);
        float z = (float) approach(from.z(), to.z(), factor);
        return new Vector3f(x, y, z);
    }

    /**
     * 将一个坐标向指定位置移动一段距离
     *
     * @param start    起始位置
     * @param target   目标位置
     * @param distance 移动的距离
     * @return 移动后的新位置
     */
    public static Vec3 move(Vec3 start, Vec3 target, double distance) {
        Vec3 step = target.subtract(start).normalize().multiply(distance, distance, distance);
        return start.add(step);
    }

    public static Vector3f move(Vector3f start, Vector3f target, float distance) {
        Vector3f step = new Vector3f(target).sub(start).normalize().mul(distance, distance, distance);
        return new Vector3f(start).add(step);
    }

    /**
     * 判断三个点是否在同一条直线上
     *
     * @author DeepSeek
     */
    public static boolean collinear(Vec3 first, Vec3 second, Vec3 third) {
        double dx1 = second.x - first.x;
        double dy1 = second.y - first.y;
        double dz1 = second.z - first.z;
        double dx2 = third.x - first.x;
        double dy2 = third.y - first.y;
        double dz2 = third.z - first.z;
        double cx = dy1 * dz2 - dz1 * dy2;
        double cy = dz1 * dx2 - dx1 * dz2;
        double cz = dx1 * dy2 - dy1 * dx2;
        double epsilon = 1e-9;
        return Math.abs(cx) < epsilon && Math.abs(cy) < epsilon && Math.abs(cz) < epsilon;
    }

    /**
     * 计算{@code current}相对于({@code end}-{@code start})区间长度的比例
     */
    public static double normalize(double current, double start, double end) {
        return current / (end - start);
    }

    /**
     * @return 返回两个坐标水平方向的距离
     */
    public static double horizontalDistance(Vec3 a, Vec3 b) {
        double x = a.x - b.x;
        double y = a.z - b.z;
        return Math.sqrt(x * x + y * y);
    }

    /**
     * @return 返回两个坐标垂直方向的距离
     */
    public static double verticalDistance(Vec3 a, Vec3 b) {
        return Math.abs(a.y() - b.y());
    }

    @SuppressWarnings("unused")
    public static Vector3f signum(Vector3f vector3f) {
        return new Vector3f(Math.signum(vector3f.x), Math.signum(vector3f.y), Math.signum(vector3f.z));
    }

    /**
     * @return 获取数组中的随机元素
     */
    public static <T> T getRandomElement(T[] array) {
        int len = array.length;
        return array[RANDOM.nextInt(len)];
    }

    /**
     * @return 获取集合中的随机元素
     */
    public static <T> T getRandomElement(List<T> list) {
        int len = list.size();
        return list.get(RANDOM.nextInt(len));
    }

    /**
     * 等差数列求和
     *
     * @param start  等差数列的首项
     * @param end    等差数列的末项
     * @param number 等差数列的项数
     * @return 等差数列的和
     */
    public static int sumOfArithmeticProgression(int start, int end, int number) {
        return ((start + end) * number) >>> 1;
    }

    public static double sumOfArithmeticProgression(double start, double end, int number) {
        return ((start + end) * number) / 2;
    }

    /**
     * 获取最接近中心位置的方块坐标
     */
    public static BlockPos calculateTheGeometricCenter(Set<BlockPos> set) {
        return switch (set.size()) {
            case 0 -> throw new IllegalArgumentException();
            case 1, 2 -> set.iterator().next();
            default -> {
                Iterator<BlockPos> iterator = set.iterator();
                BlockPos center = iterator.next();
                double minDistance = Double.MAX_VALUE;
                int minX = center.getX();
                int minY = center.getY();
                int minZ = center.getZ();
                int maxX = center.getX();
                int maxY = center.getY();
                int maxZ = center.getZ();
                while (iterator.hasNext()) {
                    BlockPos blockPos = iterator.next();
                    minX = Math.min(blockPos.getX(), minX);
                    minY = Math.min(blockPos.getY(), minY);
                    minZ = Math.min(blockPos.getZ(), minZ);
                    maxX = Math.max(blockPos.getX(), maxX);
                    maxY = Math.max(blockPos.getY(), maxY);
                    maxZ = Math.max(blockPos.getZ(), maxZ);
                }
                // 坐标的最中心方块位置
                BlockPos absoluteCenter = new BlockPos((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);
                for (BlockPos blockPos : set) {
                    double currentDistance = getBlockDistance(absoluteCenter, blockPos);
                    if (currentDistance < minDistance) {
                        center = blockPos;
                        minDistance = currentDistance;
                    }
                }
                yield center;
            }
        };
    }

    /**
     * 遍历上下左右前后，以及棱和角对着的方块
     */
    public static void allDirection(BlockPos blockPos, Consumer<BlockPos> consumer) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    consumer.accept(blockPos.offset(x, y, z));
                }
            }
        }
    }

    /**
     * 尝试将字符串解析为整数
     */
    public static OptionalInt tryParseInt(String str) {
        try {
            return OptionalInt.of(Integer.parseInt(str));
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    public static int rgba(@Range(from = 0, to = 255) int red, @Range(from = 0, to = 255) int green, @Range(from = 0, to = 255) int blue, @Range(from = 0, to = 255) int alpha) {
        return (red & 0xFF) << 24 | (green & 0xFF) << 16 | (blue & 0xFF) << 8 | (alpha & 0xFF);
    }

    public static int rgba(int rgb, int alpha) {
        return rgba(red(rgb << 8), green(rgb << 8), blue(rgb << 8), alpha);
    }

    public static int red(int rgba) {
        return rgba >> 24 & 0xFF;
    }

    public static int green(int rgba) {
        return rgba >> 16 & 0xFF;
    }

    public static int blue(int rgba) {
        return rgba >> 8 & 0xFF;
    }

    public static int alpha(int rgba) {
        return rgba & 0xFF;
    }

    public static int clamp(int value, int min, int max) {
        return Math.clamp(value, min, max);
    }
}