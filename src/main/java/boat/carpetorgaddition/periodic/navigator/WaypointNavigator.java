package boat.carpetorgaddition.periodic.navigator;

import boat.carpetorgaddition.network.s2c.WaypointUpdateS2CPacket;
import boat.carpetorgaddition.util.MathUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.Waypoint;
import boat.carpetorgaddition.wheel.provider.TextProvider;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WaypointNavigator extends AbstractNavigator {
    private final Waypoint waypoint;
    /**
     * 路径点所在维度的ID
     */
    private final Level world;
    /**
     * 玩家所在的世界
     */
    @NotNull
    private Level playerWorld;
    /**
     * 玩家所在的上一个维度
     */
    @NotNull
    private Level prevPlayerWorld;
    private final BlockPos target;
    @Nullable
    private final BlockPos secondTarget;

    public WaypointNavigator(@NotNull ServerPlayer player, Waypoint waypoint) {
        super(player);
        this.waypoint = waypoint;
        BlockPos blockPos = this.waypoint.getBlockPos();
        if (blockPos == null) {
            throw new NullPointerException();
        }
        this.target = blockPos;
        this.secondTarget = this.waypoint.getAnotherBlockPos();
        this.world = waypoint.getWorld();
        this.playerWorld = ServerUtils.getWorld(player);
        this.prevPlayerWorld = this.world;
    }

    @Override
    public void tick() {
        this.playerWorld = ServerUtils.getWorld(this.player);
        // 玩家所在的方块位置
        BlockPos playerPos = this.player.blockPosition();
        // 玩家所在维度
        if (this.world.equals(this.playerWorld)) {
            // 玩家和路径点在相同的维度
            Component display = IN.translate(waypoint.getName(), TextProvider.simpleBlockPos(this.target));
            int distance = MathUtils.getBlockIntegerDistance(playerPos, this.target);
            Component text = this.getHUDText(ServerUtils.getBlockCenter(this.target), display, distance);
            MessageUtils.sendMessageToHud(this.player, text);
        } else {
            if (this.canMapping(this.secondTarget)) {
                // 玩家和路径点在不同的维度，但是两个世界的坐标可以互相转换
                TextBuilder builder = TextBuilder.of(TextProvider.simpleBlockPos(this.secondTarget));
                // 将坐标设置为斜体
                builder.setItalic();
                Component in = IN.translate(waypoint.getName(), builder.build());
                int distance = MathUtils.getBlockIntegerDistance(playerPos, this.secondTarget);
                Component text = this.getHUDText(ServerUtils.getBlockCenter(this.secondTarget), in, distance);
                MessageUtils.sendMessageToHud(this.player, text);
            } else {
                // 玩家和路径点在不同维度
                Component dimensionName = TextProvider.dimension(ServerUtils.getWorld(ServerUtils.getServer(this.player), this.waypoint.getWorldAsString()));
                Component in = IN.translate(waypoint.getName(), TextBuilder.combineAll(dimensionName, TextProvider.simpleBlockPos(this.target)));
                MessageUtils.sendMessageToHud(this.player, in);
            }
        }
        this.syncWaypoint(false);
        this.prevPlayerWorld = this.playerWorld;
    }

    @Contract("null -> false")
    private boolean canMapping(BlockPos second) {
        if (second == null) {
            return false;
        }
        return ServerUtils.canMappingPos(this.world, this.playerWorld);
    }

    @Override
    protected WaypointUpdateS2CPacket createPacket() {
        if (this.canMapping(this.secondTarget)) {
            return new WaypointUpdateS2CPacket(this.secondTarget, this.playerWorld);
        }
        return new WaypointUpdateS2CPacket(this.target, this.world);
    }

    @Override
    protected boolean updateRequired() {
        return !this.prevPlayerWorld.equals(this.playerWorld);
    }

    @Override
    public boolean isArrive() {
        if (this.playerWorld.equals(this.world) && MathUtils.getBlockIntegerDistance(this.player.blockPosition(), this.waypoint.getBlockPos()) <= 8) {
            // 到达目的地，停止追踪
            MessageUtils.sendMessageToHud(this.player, REACH.translate());
            this.clear();
            return true;
        }
        return false;
    }

    @Override
    public WaypointNavigator copy(ServerPlayer player) {
        if (this.waypoint.getBlockPos() == null) {
            return null;
        }
        return new WaypointNavigator(player, this.waypoint);
    }
}
