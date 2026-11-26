package boat.carpetorgaddition.util;

import boat.carpetorgaddition.CarpetOrgAdditionConstants;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import carpet.CarpetServer;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.FileNameDateFormatter;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Contract;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@SuppressWarnings("unused")
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
    public static final String UNREGISTERED = "[<unregistered>]";

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

    /**
     * @return 当前游戏的NBT数据版本
     */
    @Contract(pure = true)
    public static int getMinecraftDataVersion() {
        return SharedConstants.getCurrentVersion().dataVersion().version();
    }

    /**
     * 根据维度获取世界对象
     *
     * @param server 游戏当前的服务器
     * @param id     维度的id
     */
    public static ServerLevel getWorld(MinecraftServer server, String id) {
        Identifier identifier = Identifier.parse(id);
        return Objects.requireNonNull(server.getLevel(ResourceKey.create(Registries.DIMENSION, identifier)));
    }

    public static ServerLevel getWorld(MinecraftServer server, ResourceKey<Level> key) {
        return Objects.requireNonNull(server.getLevel(key));
    }

    public static MinecraftServer getServer(ServerPlayer player) {
        return getWorld(player).getServer();
    }

    public static Optional<MinecraftServer> getServer(Entity entity) {
        return Optional.ofNullable(getWorld(entity).getServer());
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

    public static MinecraftServer getCurrentServerOrThrow() {
        return getCurrentServer().orElseThrow(() -> new IllegalStateException("Server not started"));
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

    public static boolean isUuidString(String uuid) {
        return uuidFromString(uuid).isPresent();
    }

    public static Optional<UUID> uuidFromString(String str) {
        if (str.length() != 36) {
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

    public static void forEachPlayer(MinecraftServer server, Consumer<ServerPlayer> consumer) {
        server.getPlayerList().getPlayers().forEach(consumer);
    }

    public static void forEachRealPlayer(MinecraftServer server, Consumer<ServerPlayer> consumer) {
        server.getPlayerList().getPlayers().stream().filter(PlayerUtils::isRealPlayer).forEach(consumer);
    }

    public static String getCurrentSystemTimeFormat() {
        return LocalDateTime.now().format(FileNameDateFormatter.FORMATTER);
    }

    /**
     * @return 获取当前游戏刻
     */
    public static long getCurrentGameTick(MinecraftServer server) {
        ServerLevel level = server.getLevel(Level.OVERWORLD);
        return level == null ? -1L : level.getGameTime();
    }

    /**
     * @return 服务器是否正在运行
     */
    public static boolean isRunning(MinecraftServer server) {
        return server.isRunning();
    }

    /**
     * @return 服务器是否停止运行
     */
    public static boolean isStopping(MinecraftServer server) {
        // server.stopped没有被volatile修饰，它可能不是线程安全的
        return !server.isRunning();
    }

    /**
     * @return 玩家数据是否存在
     */
    public static boolean isPlayerDataExists(MinecraftServer server, UUID uuid) {
        String filename = uuid + ".dat";
        Path path = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(filename);
        return Files.exists(path);
    }

    public static void schedule(MinecraftServer server, Runnable runnable) {
        server.schedule(new TickTask(server.getTickCount(), runnable));
    }
}
