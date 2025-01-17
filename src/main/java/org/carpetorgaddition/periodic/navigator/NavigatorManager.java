package org.carpetorgaddition.periodic.navigator;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.network.s2c.WaypointClearS2CPacket;
import org.carpetorgaddition.periodic.PlayerPeriodicTaskManager;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.wheel.Waypoint;
import org.jetbrains.annotations.Nullable;

public class NavigatorManager {
    private AbstractNavigator navigator;
    private final ServerPlayerEntity player;

    public NavigatorManager(ServerPlayerEntity player) {
        this.player = player;
    }

    public void tick() {
        if (this.navigator == null) {
            return;
        }
        try {
            this.navigator.tick();
        } catch (RuntimeException e) {
            MessageUtils.sendErrorMessage(this.player.getCommandSource(), e, "carpet.commands.navigate.exception");
            CarpetOrgAddition.LOGGER.error("导航器没有按照预期工作", e);
            // 清除导航器
            this.clearNavigator();
        }
    }

    @Nullable
    public AbstractNavigator getNavigator() {
        return this.navigator;
    }

    public void setNavigator(Entity entity, boolean isContinue) {
        this.navigator = new EntityNavigator(this.player, entity, isContinue);
    }

    public void setNavigator(Waypoint waypoint) {
        this.navigator = new WaypointNavigator(this.player, waypoint);
    }

    public void setNavigator(BlockPos blockPos, World world) {
        this.navigator = new BlockPosNavigator(this.player, blockPos, world);
    }

    public void setNavigator(BlockPos blockPos, World world, Text name) {
        this.navigator = new HasNamePosNavigator(this.player, blockPos, world, name);
    }

    public void clearNavigator() {
        this.navigator = null;
        ServerPlayNetworking.send(this.player, new WaypointClearS2CPacket());
    }

    public void setNavigatorFromOldPlayer(ServerPlayerEntity oldPlayer) {
        NavigatorManager manager = PlayerPeriodicTaskManager.getManager(oldPlayer).getNavigatorManager();
        this.navigator = manager.getNavigator();
    }
}
