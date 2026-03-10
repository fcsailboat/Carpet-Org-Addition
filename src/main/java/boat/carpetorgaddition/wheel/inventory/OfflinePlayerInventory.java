package boat.carpetorgaddition.wheel.inventory;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.periodic.task.search.OfflinePlayerSearchTask;
import boat.carpetorgaddition.util.PlayerUtils;
import boat.carpetorgaddition.wheel.GameProfileCache;
import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.NullMarked;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

@NullMarked
public class OfflinePlayerInventory extends AbstractCustomSizeInventory {
    protected final FabricPlayerAccessor accessor;
    /**
     * 是否在打开物品栏时在日志输出打开物品栏的玩家
     */
    private boolean showLog = true;

    public OfflinePlayerInventory(FabricPlayerAccessor accessor) {
        this.accessor = accessor;
    }

    /**
     * 根据玩家名称获取玩家档案
     */
    public static Optional<GameProfile> getGameProfile(String username, MinecraftServer server) {
        try {
            GameProfileCache cache = GameProfileCache.getInstance();
            Optional<GameProfile> optional = cache.getGameProfile(username);
            if (optional.isPresent()) {
                return optional;
            }
            // 获取玩家的离线UUID
            UUID uuid = UUIDUtil.createOfflinePlayerUUID(username);
            if (playerDataExists(uuid, server)) {
                GameProfile gameProfile = new GameProfile(uuid, username);
                cache.put(gameProfile);
                return Optional.of(gameProfile);
            }
        } catch (JsonParseException | NullPointerException e) {
            // 译：读取usercache.json时出现意外问题，正在使用离线玩家UUID
            CarpetOrgAddition.LOGGER.warn("An unexpected issue occurred while reading usercache.json, using offline player UUID", e);
        }
        return Optional.empty();
    }

    public static Optional<NameAndId> getPlayerConfigEntry(UUID uuid, MinecraftServer server) {
        if (playerDataExists(uuid, server)) {
            GameProfileCache cache = GameProfileCache.getInstance();
            Optional<NameAndId> optional = cache.getPlayerConfigEntry(uuid);
            return Optional.of(optional.orElse(new NameAndId(new GameProfile(uuid, OfflinePlayerSearchTask.UNKNOWN))));
        }
        return Optional.empty();
    }

    /**
     * @return 玩家数据是否存在
     */
    public static boolean playerDataExists(UUID uuid, MinecraftServer server) {
        String filename = uuid + ".dat";
        Path path = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(filename);
        return Files.exists(path);
    }

    @Override
    public int getContainerSize() {
        return 54;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void startOpen(ContainerUser user) {
        if (user instanceof ServerPlayer player) {
            this.accessor.onOpen(player);
            if (this.showLog) {
                // 译：{}打开了离线玩家{}的物品栏
                CarpetOrgAddition.LOGGER.info(
                        "{} opened the inventory of the offline player {}.",
                        PlayerUtils.getName(player),
                        this.accessor.getPlayerConfigEntry().name()
                );
            }
        }
    }

    @Override
    public void stopOpen(ContainerUser user) {
        if (user instanceof ServerPlayer player) {
            this.accessor.onClose(player);
        }
    }

    @Override
    protected Container getInventory() {
        return this.accessor.getInventory();
    }

    public void setShowLog(boolean showLog) {
        this.showLog = showLog;
    }
}
