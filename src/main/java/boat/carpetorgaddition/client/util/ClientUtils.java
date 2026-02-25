package boat.carpetorgaddition.client.util;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientUtils {
    public static final Set<UUID> FAKE_PLAYERS = ConcurrentHashMap.newKeySet();

    static {
        ClientPlayConnectionEvents.DISCONNECT.register((_, _) -> FAKE_PLAYERS.clear());
    }

    private ClientUtils() {
    }

    /**
     * 获取游戏客户端示例
     */
    public static Minecraft getClient() {
        return Minecraft.getInstance();
    }

    /**
     * 获取集成服务器
     */
    @Nullable
    @Contract(pure = true)
    public static IntegratedServer getServer() {
        return getClient().getSingleplayerServer();
    }

    /**
     * 获取客户端玩家
     */
    @NotNull
    @Contract(pure = true)
    public static LocalPlayer getPlayer() {
        LocalPlayer player = getClient().player;
        if (player == null) {
            throw new IllegalStateException("Attempted to get client player while not in a game");
        }
        return player;
    }

    @SuppressWarnings("unused")
    public static Optional<Entity> getEntity(int id) {
        if (id == -1) {
            return Optional.empty();
        }
        return Optional.ofNullable(getWorld().getEntity(id));
    }

    /**
     * 获取玩家所处客户端世界
     */
    @NotNull
    @Contract(pure = true)
    public static ClientLevel getWorld() {
        ClientLevel world = getClient().level;
        if (world == null) {
            throw new IllegalStateException("Attempted to get client world while not in a game");
        }
        return world;
    }

    @NotNull
    @Contract(pure = true)
    public static ServerLevel getServerWorld() {
        IntegratedServer server = Objects.requireNonNull(getServer(), "Attempted to get server while not in a game");
        ResourceKey<Level> dimension = getWorld().dimension();
        return Objects.requireNonNull(server.getLevel(dimension), () -> "Invalid dimension: " + dimension.identifier());
    }

    /**
     * @return 获取游戏渲染器
     */
    @NotNull
    public static GameRenderer getGameRenderer() {
        return getClient().gameRenderer;
    }

    /**
     * 获取文字渲染器
     */
    public static Font getTextRenderer() {
        return getClient().font;
    }

    /**
     * 获取摄像机
     */
    @Contract(pure = true)
    public static Camera getCamera() {
        return getGameRenderer().getMainCamera();
    }

    @Contract(pure = true)
    public static DeltaTracker getTickCounter() {
        return getClient().getDeltaTracker();
    }

    /**
     * 获取当前打开的屏幕
     */
    @Nullable
    public static Screen getCurrentScreen() {
        return getClient().screen;
    }

    /**
     * 获取当前打开菜单
     */
    @SuppressWarnings("unused")
    @NotNull
    public static AbstractContainerMenu getContainerMenu() {
        return getPlayer().containerMenu;
    }

    /**
     * 获取客户端游戏设置
     */
    public static Options getGameOptions() {
        return getClient().options;
    }

    /**
     * 获取当前的准星指向
     */
    @Nullable
    public static HitResult getCrosshairTarget() {
        return getClient().hitResult;
    }

    /**
     * 获取鼠标信息
     */
    public static MouseHandler getMouse() {
        return getClient().mouseHandler;
    }

    // TODO 快速设置合成配方是否需要检查假玩家
    public static boolean isFakePlayer(Player player) {
        return FAKE_PLAYERS.contains(player.getUUID());
    }
}
