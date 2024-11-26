package org.carpetorgaddition.util.navigator;

import carpet.utils.CommandHelper;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.network.s2c.WaypointUpdateS2CPacket;
import org.carpetorgaddition.util.TextUtils;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractNavigator {
    protected static final String IN = "carpet.commands.navigate.hud.in";
    protected static final String DISTANCE = "carpet.commands.navigate.hud.distance";
    protected static final String REACH = "carpet.commands.navigate.hud.reach";
    @NotNull
    protected final ServerPlayerEntity player;
    protected final NavigatorInterface navigatorInterface;
    /**
     * 上一个坐标
     */
    private Vec3d previousPosition;
    /**
     * 上一个维度
     */
    private String previousWorldId;

    public AbstractNavigator(@NotNull ServerPlayerEntity player) {
        this.player = player;
        this.navigatorInterface = (NavigatorInterface) this.player;
    }

    public abstract void tick();

    /**
     * 此导航器的结束条件
     *
     * @return 导航是否需要结束
     */
    protected abstract boolean terminate();

    /**
     * @return 此导航器的浅拷贝副本
     */
    public abstract AbstractNavigator copy(ServerPlayerEntity player);

    @NotNull
    protected MutableText getHUDText(Vec3d vec3d, Text in, Text distance) {
        MutableText text;
        // 添加上下箭头
        text = switch (verticalAngle(this.player, vec3d)) {
            case 1 -> TextUtils.appendAll(in, " ↑ ", distance);
            case -1 -> TextUtils.appendAll(in, " ↓ ", distance);
            default -> TextUtils.appendAll(in, "   ", distance);
        };
        // 添加左右箭头
        text = switch (forwardAngle(this.player, vec3d)) {
            case -3 -> TextUtils.appendAll("    ", text, " >>>");
            case -2 -> TextUtils.appendAll("    ", text, "  >>");
            case -1 -> TextUtils.appendAll("    ", text, "   >");
            case 1 -> TextUtils.appendAll("<   ", text, "    ");
            case 2 -> TextUtils.appendAll("<<  ", text, "    ");
            case 3 -> TextUtils.appendAll("<<< ", text, "    ");
            default -> TextUtils.appendAll("    ", text, "    ");
        };
        return text;
    }

    /**
     * @param target 玩家看向的位置
     * @see net.minecraft.client.gui.hud.SubtitlesHud#render(DrawContext)
     */
    private static int forwardAngle(PlayerEntity player, Vec3d target) {
        double x = target.getX() - player.getX();
        double y = target.getZ() - player.getZ();
        // 将直角坐标转换为极坐标，然后获取角度
        double result = player.getYaw() + Math.toDegrees(Math.atan2(x, y));
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
    private static int verticalAngle(PlayerEntity player, Vec3d target) {
        double x = Math.sqrt(Math.pow(player.getX() - target.getX(), 2) + Math.pow(player.getZ() - target.getZ(), 2));
        double y = target.getY() - player.getEyeY();
        double result = player.getPitch() + Math.toDegrees(Math.atan2(y, x));
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
    protected void syncWaypoint(WaypointUpdateS2CPacket pack) {
        // 更新上一个坐标
        if (this.updatePrevious(pack)) {
            // 要求玩家有执行/navigate命令的权限
            boolean hasPermission = CommandHelper.canUseCommand(this.player.getCommandSource(), CarpetOrgAdditionSettings.commandNavigate);
            if (CarpetOrgAdditionSettings.syncNavigateWaypoint && hasPermission) {
                ServerPlayNetworking.send(this.player, pack);
            }
        }
    }

    /**
     * 更新上一个坐标
     *
     * @return 坐标是否更新了
     */
    private boolean updatePrevious(WaypointUpdateS2CPacket pack) {
        // 目标未移动，不需要更新
        if (pack.target().equals(this.previousPosition) && pack.worldId().equals(this.previousWorldId)) {
            return false;
        }
        this.previousPosition = pack.target();
        this.previousWorldId = pack.worldId();
        return true;
    }

    /**
     * 发送路径点更新
     */
    public void sendWaypointUpdate() {
        if (this.previousPosition == null || this.previousWorldId == null) {
            return;
        }
        ServerPlayNetworking.send(this.player, new WaypointUpdateS2CPacket(this.previousPosition, this.previousWorldId));
    }

    /**
     * 让玩家清除这个导航器
     */
    public void clear() {
        this.navigatorInterface.clearNavigator();
    }
}
