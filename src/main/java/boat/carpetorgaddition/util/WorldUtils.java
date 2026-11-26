package boat.carpetorgaddition.util;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
public class WorldUtils {
    public static ResourceKey<Level> getWorldKey(Identifier id) {
        return ResourceKey.create(Registries.DIMENSION, id);
    }

    public static ResourceKey<Level> getWorldKey(String worldId) {
        return getWorldKey(Identifier.parse(worldId));
    }

    public static ResourceKey<Level> getWorldKey(Level world) {
        return world.dimension();
    }

    /**
     * 根据维度获取对应的颜色
     *
     * @return 如果是下界，返回红色；如果是末地，返回紫色；如果是主世界或者自定义维度，返回绿色
     */
    public static ChatFormatting getColor(Level world) {
        if (isTheNether(world)) {
            return ChatFormatting.RED;
        }
        if (isTheEnd(world)) {
            return ChatFormatting.DARK_PURPLE;
        }
        return ChatFormatting.GREEN;
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
    public static String toWorldPosString(Level world, BlockPos blockPos) {
        return IdentityUtils.getIdAsString(world) + "[" + toPosString(blockPos) + "]";
    }

    /**
     * 在指定位置播放一个音效
     */
    public static void playSound(Level world, BlockPos blockPos, SoundEvent soundEvent, SoundSource soundCategory) {
        world.playSound(null, blockPos, soundEvent, soundCategory, 1F, 1F);
    }

    public static void playSound(Level world, Vec3 vec3, SoundEvent event, SoundSource source) {
        world.playSound(null, vec3.x(), vec3.y(), vec3.z(), event, source);
    }

    /**
     * 在指定玩家位置播放一个音效
     *
     * @param soundEvent    声音时间
     * @param soundCategory 声音类别
     */
    public static void playSound(ServerPlayer player, SoundEvent soundEvent, SoundSource soundCategory) {
        Level world = player.level();
        world.playSound(null, player.getX(), player.getY(), player.getZ(), soundEvent, soundCategory, 1F, 1F);
    }

    public static void playSound(ServerPlayer player, Holder<SoundEvent> soundEvent, SoundSource soundCategory) {
        Level world = player.level();
        world.playSound(null, player.getX(), player.getY(), player.getZ(), soundEvent, soundCategory, 1F, 1F);
    }

    /**
     * @return 两个世界的坐标是否可以互相转换
     */
    public static boolean canMappingPos(Level world1, Level world2) {
        return (isOverworld(world1) && isTheNether(world2)) || (isOverworld(world2) && isTheNether(world1));
    }

    /**
     * @return 最大建筑高度
     */
    public static int getMaxArchitectureAltitude(Level world) {
        return world.getMaxY();
    }

    /**
     * @return 最小建筑高度
     */
    public static int getMinArchitectureAltitude(Level world) {
        return world.getMinY();
    }

    /**
     * 将一个实体传送到目标维度的指定位置
     *
     * @param source 要传送的实体
     * @param world  要传送到目标维度
     * @param x      目的地X坐标
     * @param y      目的地Y坐标
     * @param z      目的地Z坐标
     * @param yaw    传送后实体的偏航角
     * @param pitch  传送后实体的俯仰角
     */
    public static void teleport(Entity source, ServerLevel world, double x, double y, double z, float yaw, float pitch) {
        source.teleportTo(world, x, y, z, Set.of(), yaw, pitch, true);
    }

    /**
     * 将指定实体传送到目标实体位置
     *
     * @param source 要传送的实体
     * @param target 目标实体
     */
    public static void teleport(Entity source, Entity target) {
        // 不要在客户端传送实体
        if (target.level() instanceof ServerLevel world) {
            teleport(source, world, target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
        }
    }

    public static void teleport(Entity entity, ServerLevel world, Vec3 pos) {
        teleport(entity, world, pos.x(), pos.y(), pos.z(), entity.getYRot(), entity.getXRot());
    }

    /**
     * 将一个实体传送到目标维度的指定位置并显示粒子和播放音效
     */
    public static void teleportWithEffect(Entity entity, ServerLevel world, Vec3 pos) {
        teleportWithEffect(entity, world, pos, entity.getYRot(), entity.getXRot());
    }

    public static void teleportIfServerWithEffect(Entity entity, Entity target) {
        if (entity.level() instanceof ServerLevel world) {
            teleportWithEffect(entity, world, target.position(), target.getYRot(), target.getXRot());
        }
    }

    public static void teleportWithEffect(Entity entity, ServerLevel world, Vec3 pos, float yaw, float pitch) {
        teleport(entity, world, pos.x(), pos.y(), pos.z(), yaw, pitch);
        world.broadcastEntityEvent(entity, EntityEvent.TELEPORT);
        SoundEvent soundEvent = switch (entity) {
            case Player _ -> SoundEvents.PLAYER_TELEPORT;
            case Fox _ -> SoundEvents.FOX_TELEPORT;
            case Shulker _ -> SoundEvents.SHULKER_TELEPORT;
            case Enderman _ -> SoundEvents.ENDERMAN_TELEPORT;
            default -> SoundEvents.CHORUS_FRUIT_TELEPORT;
        };
        playSound(world, pos, soundEvent, entity.getSoundSource());
        entity.resetFallDistance();
    }

    public static void teleportIfServerWithEffect(Entity entity, Vec3 pos) {
        if (entity.level() instanceof ServerLevel world) {
            teleportWithEffect(entity, world, pos);
        }
    }

    public static Vec3 getBlockCenter(BlockPos blockPos) {
        return new Vec3(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
    }

    public static Vec3 getBlockBottomCenter(BlockPos blockPos) {
        return new Vec3(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5);
    }


    /**
     * @return 指定世界是否为主世界
     */
    public static boolean isOverworld(Level world) {
        return isOverworld(world.dimension());
    }

    /**
     * @return 指定世界是否为下界
     */
    public static boolean isTheNether(Level world) {
        return isTheNether(world.dimension());
    }

    /**
     * @return 指定世界是否为末地
     */
    public static boolean isTheEnd(Level world) {
        return isTheEnd(world.dimension());
    }

    public static boolean isOverworld(ResourceKey<Level> key) {
        return Level.OVERWORLD.equals(key);
    }

    public static boolean isTheNether(ResourceKey<Level> key) {
        return Level.NETHER.equals(key);
    }

    public static boolean isTheEnd(ResourceKey<Level> key) {
        return Level.END.equals(key);
    }

    public static BlockPos toOverworldBlockPos(BlockPos blockPos) {
        int x = blockPos.getX() * 8;
        int z = blockPos.getZ() * 8;
        return new BlockPos(x, blockPos.getY(), z);
    }

    public static BlockPos toTheNetherBlockPos(BlockPos blockPos) {
        int x = (int) Math.round(blockPos.getX() / 8.0);
        int z = (int) Math.round(blockPos.getZ() / 8.0);
        return new BlockPos(x, blockPos.getY(), z);
    }

    public static <T extends Entity> List<T> getEntities(Level world, BlockPos from, BlockPos to, Class<T> type) {
        AABB aabb = new AABB(from.getX(), from.getY(), from.getZ(), to.getX() + 1, to.getY() + 1, to.getZ() + 1);
        return getEntities(world, aabb, type);
    }

    public static <T extends Entity> List<T> getEntities(Level world, AABB aabb, Class<T> type) {
        return world.getEntitiesOfClass(type, aabb);
    }

    public static <T extends Entity> boolean hasEntityIn(Level world, BlockPos from, BlockPos to, Class<T> type) {
        AABB aabb = new AABB(from.getX(), from.getY(), from.getZ(), to.getX() + 1, to.getY() + 1, to.getZ() + 1);
        ArrayList<T> result = new ArrayList<>();
        world.getEntities(EntityTypeTest.forClass(type), aabb, EntitySelector.NO_SPECTATORS, result, 1);
        return !result.isEmpty();
    }
}
