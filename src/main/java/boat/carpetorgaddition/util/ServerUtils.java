package boat.carpetorgaddition.util;

import boat.carpetorgaddition.CarpetOrgAdditionConstants;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import carpet.CarpetServer;
import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.FileNameDateFormatter;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class ServerUtils {
    /**
     * 当前{@code Minecraft}的NBT数据版本
     */
    public static final int CURRENT_DATA_VERSION = getMinecraftDataVersion();
    public static final String OVERWORLD = "minecraft:overworld";
    public static final String THE_NETHER = "minecraft:the_nether";
    public static final String THE_END = "minecraft:the_end";
    public static final String SIMPLE_OVERWORLD = "overworld";
    public static final String SIMPLE_THE_NETHER = "the_nether";
    public static final String SIMPLE_THE_END = "the_end";

    private ServerUtils() {
    }

    /**
     * 根据UUID获取实体
     */
    public static Optional<Entity> getEntity(MinecraftServer server, UUID uuid) {
        for (ServerLevel world : server.getAllLevels()) {
            Entity entity = world.getEntity(uuid);
            if (entity != null) {
                return Optional.of(entity);
            }
        }
        return Optional.empty();
    }

    /**
     * 根据UUID获取玩家
     */
    public static Optional<ServerPlayer> getPlayer(MinecraftServer server, UUID uuid) {
        return Optional.ofNullable(server.getPlayerList().getPlayer(uuid));
    }

    /**
     * 根据名称获取玩家
     */
    public static Optional<ServerPlayer> getPlayer(MinecraftServer server, String name) {
        return Optional.ofNullable(server.getPlayerList().getPlayerByName(name));
    }

    public static Optional<ServerPlayer> getPlayer(MinecraftServer server, GameProfile gameProfile) {
        return getPlayer(server, gameProfile.name());
    }

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
     * @return 当前游戏的NBT数据版本
     */
    @Contract(pure = true)
    public static int getMinecraftDataVersion() {
        return SharedConstants.getCurrentVersion().dataVersion().version();
    }

    /**
     * 根据维度获取对应的颜色
     *
     * @return 如果是下界，返回红色；如果是末地，返回紫色；如果是主世界或者自定义维度，返回绿色
     */
    public static ChatFormatting getColor(Level world) {
        if (ServerUtils.isTheNether(world)) {
            return ChatFormatting.RED;
        }
        if (ServerUtils.isTheEnd(world)) {
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
        return getWorldIdAsString(world) + "[" + toPosString(blockPos) + "]";
    }

    /**
     * 获取当前维度的ID
     *
     * @param world 当前世界的对象
     * @return 当前维度的ID
     */
    public static String getWorldIdAsString(Level world) {
        return getWorldId(world).toString();
    }

    public static Identifier getWorldId(Level world) {
        return world.dimension().identifier();
    }

    /**
     * 从服务器寻找一个指定UUID的实体
     */
    @Nullable
    public static Entity getEntityFromUUID(MinecraftServer server, UUID uuid) {
        for (ServerLevel world : server.getAllLevels()) {
            Entity entity = world.getEntity(uuid);
            if (entity == null) {
                continue;
            }
            return entity;
        }
        return null;
    }

    /**
     * 在指定位置播放一个音效
     */
    public static void playSound(Level world, BlockPos blockPos, SoundEvent soundEvent, SoundSource soundCategory) {
        world.playSound(null, blockPos, soundEvent, soundCategory, 1F, 1F);
    }

    /**
     * 在指定玩家位置播放一个音效
     *
     * @param soundEvent    声音时间
     * @param soundCategory 声音类别
     */
    public static void playSound(ServerPlayer player, SoundEvent soundEvent, SoundSource soundCategory) {
        Level world = getWorld(player);
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

    /**
     * 让实体看向指定位置
     */
    public static void lookAt(Entity entity, Vec3 pos) {
        lookAt(entity, EntityAnchorArgument.Anchor.EYES, pos);
    }

    public static void lookAt(Entity entity, EntityAnchorArgument.Anchor anchor, Vec3 pos) {
        entity.lookAt(anchor, pos);
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

    /**
     * 根据维度获取世界对象
     *
     * @param server 游戏当前的服务器
     * @param id     维度的id
     */
    public static ServerLevel getWorld(MinecraftServer server, String id) {
        Identifier identifier = Identifier.parse(id);
        return server.getLevel(ResourceKey.create(Registries.DIMENSION, identifier));
    }

    @Contract("_ -> !null")
    public static MinecraftServer getServer(ServerPlayer player) {
        return getWorld(player).getServer();
    }

    @Nullable
    public static MinecraftServer getServer(Entity entity) {
        return getWorld(entity).getServer();
    }

    public static MinecraftServer getServer(CommandSourceStack source) {
        return source.getServer();
    }

    public static Optional<MinecraftServer> getCurrentServer() {
        if (ServerComponentCoordinator.SERVER_INSTANCE.isBound()) {
            return Optional.of(ServerComponentCoordinator.SERVER_INSTANCE.get());
        }
        return Optional.ofNullable(CarpetServer.minecraft_server);
    }

    public static ServerLevel getWorld(ServerPlayer player) {
        return player.level();
    }

    public static Level getWorld(Entity entity) {
        return entity.level();
    }

    public static ServerLevel getWorld(CommandSourceStack source) {
        return source.getLevel();
    }

    public static Level getWorld(BlockEntity blockEntity) {
        return blockEntity.getLevel();
    }

    public static Vec3 getFootPos(Entity entity) {
        return entity.position();
    }

    public static Vec3 getEyePos(Entity entity) {
        return entity.getEyePosition();
    }

    public static Component getName(Item item) {
        return item.components().getOrDefault(DataComponents.ITEM_NAME, CommonComponents.EMPTY);
    }

    public static Component getDefaultName(ItemStack itemStack) {
        return getName(itemStack.getItem());
    }

    public static String currentTimeFormat() {
        return LocalDateTime.now().format(FileNameDateFormatter.FORMATTER);
    }

    public static Identifier getId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }

    public static Identifier getId(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block);
    }

    @SuppressWarnings("unused")
    public static Optional<Identifier> getId(Level world, Enchantment enchantment) {
        Holder<Enchantment> entry = Holder.direct(enchantment);
        entry.unwrapKey().map(ResourceKey::identifier);
        return getId(world.registryAccess(), enchantment);
    }

    public static Optional<Identifier> getId(MinecraftServer server, Enchantment enchantment) {
        return getId(server.registryAccess(), enchantment);
    }

    public static Optional<Identifier> getId(RegistryAccess registryManager, Enchantment enchantment) {
        Optional<Registry<Enchantment>> optional = registryManager.lookup(Registries.ENCHANTMENT);
        if (optional.isEmpty()) {
            return Optional.empty();
        }
        Registry<Enchantment> enchantments = optional.get();
        return Optional.ofNullable(enchantments.getKey(enchantment));
    }

    public static String getIdAsString(Item item) {
        return getId(item).toString();
    }

    public static String getIdAsString(Block block) {
        return getId(block).toString();
    }

    /**
     * 将字符串ID转换为物品
     */
    public static Item asItem(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
    }

    /**
     * 将字符串ID转换为方块
     */
    public static Block asBlock(String id) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
    }

    @SuppressWarnings("unused")
    public static Optional<UUID> uuidFromString(@Nullable String str) {
        if (str == null || str.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(str));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static Identifier ofIdentifier(String id) {
        return Identifier.fromNamespaceAndPath(CarpetOrgAdditionConstants.MOD_ID, id);
    }
}
