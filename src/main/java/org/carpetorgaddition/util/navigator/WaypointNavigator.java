package org.carpetorgaddition.util.navigator;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.carpetorgaddition.network.s2c.WaypointUpdateS2CPacket;
import org.carpetorgaddition.util.MathUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.WorldUtils;
import org.carpetorgaddition.util.constant.TextConstants;
import org.carpetorgaddition.util.wheel.Waypoint;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class WaypointNavigator extends AbstractNavigator {
    private final Waypoint waypoint;

    // 路径点所在维度的ID
    private final String waypointDimension;

    public WaypointNavigator(@NotNull ServerPlayerEntity player, Waypoint waypoint) {
        super(player);
        this.waypoint = waypoint;
        if (this.waypoint.getBlockPos() == null) {
            throw new NullPointerException();
        }
        this.waypointDimension = waypoint.getDimension();
    }

    @Override
    public void tick() {
        if (terminate()) {
            this.clear();
            return;
        }
        // 路径点的目标位置
        BlockPos blockPos = this.waypoint.getBlockPos();
        // 玩家所在的方块位置
        BlockPos playerBlockPos = this.player.getBlockPos();
        // 玩家所在维度
        String playerDimension = WorldUtils.getDimensionId(this.player.getWorld());
        if (playerDimension.equals(waypointDimension)) {
            // 玩家和路径点在相同的维度
            Text text = this.getHUDText(blockPos.toCenterPos(), getIn(blockPos), getDistance(playerBlockPos, blockPos));
            MessageUtils.sendMessageToHud(this.player, text);
            this.syncWaypoint(new WaypointUpdateS2CPacket(blockPos.toCenterPos(), waypointDimension));
        } else {
            BlockPos anotherBlockPos = this.waypoint.getAnotherBlockPos();
            if (((playerDimension.equals(WorldUtils.OVERWORLD) && waypointDimension.equals(WorldUtils.THE_NETHER))
                    || (playerDimension.equals(WorldUtils.THE_NETHER) && waypointDimension.equals(WorldUtils.OVERWORLD)))
                    && anotherBlockPos != null) {
                // 玩家和路径点在不同的维度，但是维度可以互相转换
                // 将坐标设置为斜体
                Text in = TextUtils.translate(IN, waypoint.getName(),
                        TextUtils.toItalic(TextConstants.simpleBlockPos(blockPos)));
                Text text = this.getHUDText(anotherBlockPos.toCenterPos(), in,
                        getDistance(playerBlockPos, anotherBlockPos));
                MessageUtils.sendMessageToHud(this.player, text);
                this.syncWaypoint(new WaypointUpdateS2CPacket(anotherBlockPos.toCenterPos(), playerDimension));
            } else {
                // 玩家和路径点在不同维度
                Text dimensionName = WorldUtils.getDimensionName(WorldUtils.getWorld(this.player.getServer(),
                        this.waypoint.getDimension()));
                MutableText in = TextUtils.translate(IN, waypoint.getName(),
                        TextUtils.appendAll(dimensionName, TextConstants.simpleBlockPos(blockPos)));
                MessageUtils.sendMessageToHud(this.player, in);
            }
        }
    }

    @Override
    public boolean terminate() {
        if (Objects.equals(WorldUtils.getDimensionId(this.player.getWorld()), this.waypointDimension)
                && MathUtils.getBlockIntegerDistance(this.player.getBlockPos(), this.waypoint.getBlockPos()) <= 8) {
            // 到达目的地，停止追踪
            MessageUtils.sendMessageToHud(this.player, TextUtils.translate(REACH));
            this.clear();
            return true;
        }
        return false;
    }

    @Override
    public WaypointNavigator copy(ServerPlayerEntity player) {
        if (this.waypoint == null || this.waypoint.getBlockPos() == null) {
            return null;
        }
        return new WaypointNavigator(player, this.waypoint);
    }

    @NotNull
    private MutableText getIn(BlockPos blockPos) {
        return TextUtils.translate(IN, waypoint.getName(), TextConstants.simpleBlockPos(blockPos));
    }

    @NotNull
    private static MutableText getDistance(BlockPos playerBlockPos, BlockPos blockPos) {
        return TextUtils.translate(DISTANCE, MathUtils.getBlockIntegerDistance(playerBlockPos, blockPos));
    }
}
