package org.carpetorgaddition.util;

import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.carpetorgaddition.util.constant.TextConstants;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.UUID;

public class WorldUtils {
    private WorldUtils() {
    }

    public static final String OVERWORLD = "minecraft:overworld";
    public static final String THE_NETHER = "minecraft:the_nether";
    public static final String THE_END = "minecraft:the_end";
    public static final String SIMPLE_OVERWORLD = "overworld";
    public static final String SIMPLE_THE_NETHER = "the_nether";
    public static final String SIMPLE_THE_END = "the_end";

    /**
     * 获取区域内所有方块坐标的集合
     *
     * @param box 用来指定的区域盒子对象
     * @return 盒子内所有的方块坐标
     * @see org.carpetorgaddition.util.wheel.SelectionArea
     * @deprecated 如果Box对象的范围比较大，则会将这个范围内的所有方块坐标对象全部返回，
     * 这对内存是一个较大的负担。例如：如果Box的范围是长宽高各256，那么将有256*256*256=16777216个对象被创建，
     * 并且在集合对象使用完毕之前不会被回收，短时间内多次调用时，容易导致{@link OutOfMemoryError}
     */
    @Deprecated(forRemoval = true)
    public static ArrayList<BlockPos> allBlockPos(Box box) {
        int endX = (int) box.maxX;
        int endY = (int) box.maxY;
        int endZ = (int) box.maxZ;
        ArrayList<BlockPos> list = new ArrayList<>();
        for (int startX = (int) box.minX; startX < endX; startX++) {
            for (int startY = (int) box.minY; startY < endY; startY++) {
                for (int startZ = (int) box.minZ; startZ < endZ; startZ++) {
                    list.add(new BlockPos(startX, startY, startZ));
                }
            }
        }
        return list;
    }

    /**
     * 根据维度获取对应的颜色
     *
     * @return 如果是下界，返回红色；如果是末地，返回紫色；如果是主世界或者自定义维度，返回绿色
     */
    public static Formatting getColor(World world) {
        return switch (getDimensionId(world)) {
            case THE_NETHER -> Formatting.RED;
            case THE_END -> Formatting.DARK_PURPLE;
            default -> Formatting.GREEN;
        };
    }

    /**
     * 获取方块坐标的字符串形式
     *
     * @param blockPos 要转换为字符串形式的方块位置对象
     * @return 方块坐标的字符串形式
     */
    public static String toPosString(BlockPos blockPos) {
        return blockPos.getX() + " " + blockPos.getY() + " " + blockPos.getZ();
    }

    /**
     * 将方块坐标转换为字符串形式，坐标前追加维度id
     */
    public static String toWorldPosString(World world, BlockPos blockPos) {
        return getDimensionId(world) + "[" + toPosString(blockPos) + "]";
    }

    /**
     * 获取当前维度的ID
     *
     * @param world 当前世界的对象
     * @return 当前维度的ID
     */
    public static String getDimensionId(World world) {
        return world.getRegistryKey().getValue().toString();
    }

    /**
     * 根据维度获取世界对象
     *
     * @param server    游戏当前的服务器
     * @param dimension 一个维度的id
     */
    public static ServerWorld getWorld(MinecraftServer server, String dimension) {
        String[] split = dimension.split(":");
        Identifier identifier;
        if (split.length == 1) {
            identifier = Identifier.of("minecraft", dimension);
        } else if (split.length == 2) {
            identifier = Identifier.of(split[0], split[1]);
        } else {
            throw new IllegalArgumentException();
        }
        return server.getWorld(RegistryKey.of(RegistryKeys.WORLD, identifier));
    }

    /**
     * 将字符串解析为世界ID
     *
     * @param worldId 世界的ID，如果指定了命名空间，则使用指定的，否则使用minecraft
     * @return 世界类型的注册表项
     */
    public static RegistryKey<World> getWorld(String worldId) {
        if (worldId.contains(":")) {
            String[] split = worldId.split(":");
            if (split.length != 2) {
                throw new IllegalArgumentException();
            }
            return RegistryKey.of(RegistryKeys.WORLD, Identifier.of(split[0], split[1]));
        }
        return RegistryKey.of(RegistryKeys.WORLD, Identifier.of(worldId));
    }

    /**
     * 从服务器寻找一个指定UUID的实体
     */
    @Nullable
    public static Entity getEntityFromUUID(MinecraftServer server, UUID uuid) {
        for (ServerWorld world : server.getWorlds()) {
            Entity entity = world.getEntity(uuid);
            if (entity == null) {
                continue;
            }
            return entity;
        }
        return null;
    }

    /**
     * 获取维度名称
     *
     * @param world 要获取维度名称的世界对象
     * @return 如果是原版的3个维度，返回本Mod翻译后的名称，否则自己返回维度ID
     */
    public static Text getDimensionName(World world) {
        String dimension = WorldUtils.getDimensionId(world);
        return switch (dimension) {
            case OVERWORLD -> TextConstants.OVERWORLD;
            case THE_NETHER -> TextConstants.THE_NETHER;
            case THE_END -> TextConstants.THE_END;
            default -> TextUtils.createText(dimension);
        };
    }

    /**
     * 在指定位置播放一个音效
     */
    public static void playSound(World world, BlockPos blockPos, SoundEvent soundEvent, SoundCategory soundCategory) {
        world.playSound(null, blockPos, soundEvent, soundCategory, 1F, 1F);
    }

    /**
     * 在指定玩家位置播放一个音效
     *
     * @param soundEvent    声音时间
     * @param soundCategory 声音类别
     */
    public static void playSound(ServerPlayerEntity player, SoundEvent soundEvent, SoundCategory soundCategory) {
        World world = player.getWorld();
        world.playSound(null, player.getX(), player.getY(), player.getZ(), soundEvent, soundCategory, 1F, 1F);
    }

    /**
     * @return 两个维度ID是否表示的是同一个世界
     * @throws InvalidIdentifierException 如果维度ID不合法
     */
    public static boolean equalsWorld(String world1, String world2) {
        return Identifier.of(world1).equals(Identifier.of(world2));
    }

    /**
     * @return 两个维度ID是否分别表示不同的世界
     * @throws InvalidIdentifierException 如果维度ID不合法
     */
    public static boolean differentWorld(String world1, String world2) {
        return !Identifier.of(world1).equals(Identifier.of(world2));
    }

    /**
     * @return 维度ID是否表示主世界
     */
    public static boolean isOverworld(String worldId) {
        return OVERWORLD.equals(worldId) || SIMPLE_OVERWORLD.equals(worldId);
    }

    /**
     * @return 维度ID是否表示下界
     */
    public static boolean isTheNether(String worldId) {
        return THE_NETHER.equals(worldId) || SIMPLE_THE_NETHER.equals(worldId);
    }

    /**
     * @return 维度ID是否表示末地
     */
    public static boolean isTheEnd(String worldId) {
        return THE_END.equals(worldId) || SIMPLE_THE_END.equals(worldId);
    }
}
