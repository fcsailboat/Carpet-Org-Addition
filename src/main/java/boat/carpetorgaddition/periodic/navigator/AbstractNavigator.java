package boat.carpetorgaddition.periodic.navigator;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.command.NavigatorCommand;
import boat.carpetorgaddition.network.s2c.WaypointUpdateS2CPacket;
import boat.carpetorgaddition.periodic.PlayerComponentCoordinator;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.TextJoiner;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.SubtitleOverlay;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

import java.util.Map;

public abstract class AbstractNavigator {
    protected static final LocalizationKey IN = NavigatorCommand.HUD.then("in");
    protected static final LocalizationKey REACH = NavigatorCommand.HUD.then("reach");
    protected final ServerPlayer player;
    protected final MinecraftServer server;
    protected final NavigatorManager manager;

    public AbstractNavigator(ServerPlayer player) {
        this.player = player;
        this.server = ServerUtils.getServer(player);
        this.manager = PlayerComponentCoordinator.getCoordinator(this.player).getNavigatorManager();
    }

    /**
     * 开始导航时调用
     */
    public void onStart() {
        this.syncWaypoint(true);
    }

    /**
     * 每个游戏刻都调用
     */
    public abstract void tick();

    /**
     * 此导航器的结束条件
     *
     * @return 导航是否需要结束
     */
    protected abstract boolean isArrive();

    /**
     * @return 此导航器的浅拷贝副本
     */
    public abstract AbstractNavigator copy(ServerPlayer player);

    @NonNull
    protected Component getHUDText(Vec3 vec3d, Component displayName, int distance) {
        // 添加左右箭头
        Map.Entry<String, String> entry = switch (forwardAngle(this.player, vec3d)) {
            case -3 -> Map.entry("    ", " >>>");
            case -2 -> Map.entry("    ", "  >>");
            case -1 -> Map.entry("    ", "   >");
            case 1 -> Map.entry("<   ", "    ");
            case 2 -> Map.entry("<<  ", "    ");
            case 3 -> Map.entry("<<< ", "    ");
            default -> Map.entry("    ", "    ");
        };
        TextJoiner joiner = new TextJoiner();
        joiner.append(entry.getKey());
        joiner.append(displayName);
        // 添加上下箭头
        joiner.append(switch (verticalAngle(this.player, vec3d)) {
            case 1 -> " ↑ ";
            case -1 -> " ↓ ";
            default -> "   ";
        });
        joiner.append(NavigatorCommand.HUD.then("distance").translate(distance));
        joiner.append(entry.getValue());
        return joiner.join();
    }

    /**
     * @param target 玩家看向的位置
     * @see SubtitleOverlay#extractRenderState(GuiGraphicsExtractor)
     */
    private static int forwardAngle(Player player, Vec3 target) {
        double x = target.x() - player.getX();
        double y = target.z() - player.getZ();
        // 将直角坐标转换为极坐标，然后获取角度
        double result = player.getYRot() + Math.toDegrees(Math.atan2(x, y));
        result = result < 0 ? result + 360 : result;
        result = result > 180 ? result - 360 : result;
        return forwardAngle(result);
    }

    private static int forwardAngle(double value) {
        if (value < 0) {
            return -forwardAngle(-value);
        }
        if (value <= 3) {
            return 0;
        }
        if (value <= 60) {
            return 1;
        }
        if (value <= 100) {
            return 2;
        }
        return 3;
    }

    // 玩家视角是否指向目标位置（仅考虑高度）
    private static int verticalAngle(Player player, Vec3 target) {
        double x = Math.sqrt(Math.pow(player.getX() - target.x(), 2) + Math.pow(player.getZ() - target.z(), 2));
        double y = target.y() - player.getEyeY();
        double result = player.getXRot() + Math.toDegrees(Math.atan2(y, x));
        if (result >= 10) {
            return 1;
        } else if (result <= -10) {
            return -1;
        } else {
            return 0;
        }
    }

    /**
     * 同步路径点
     */
    public void syncWaypoint(boolean force) {
        // 更新上一个坐标
        if ((force || this.updateRequired()) && CarpetOrgAdditionSettings.SYNC_NAVIGATE_WAYPOINT.value()) {
            WaypointUpdateS2CPacket packet = this.createPacket();
            ServerPlayNetworking.send(this.player, packet);
        }
    }

    /**
     * 创建同步数据包
     */
    protected abstract WaypointUpdateS2CPacket createPacket();

    /**
     * @return 坐标是否需要更新
     */
    protected abstract boolean updateRequired();

    /**
     * 让玩家清除这个导航器
     */
    public void clear() {
        this.manager.clearNavigator();
    }
}
