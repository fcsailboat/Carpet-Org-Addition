package boat.carpetorgaddition.periodic.navigator;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.command.NavigatorCommand;
import boat.carpetorgaddition.network.s2c.WaypointClearS2CPacket;
import boat.carpetorgaddition.periodic.PlayerComponentCoordinator;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.wheel.Waypoint;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class NavigatorManager {
    @Nullable
    private AbstractNavigator navigator;
    private boolean updated = false;
    private final ServerPlayer player;

    public NavigatorManager(ServerPlayer player) {
        this.player = player;
    }

    public void tick() {
        if (this.navigator == null) {
            return;
        }
        try {
            if (this.navigator.isArrive() || !CarpetOrgAdditionSettings.COMMAND_NAVIGATE.value().hasPermission(this.player)) {
                this.clearNavigator();
                return;
            }
            if (this.updated) {
                this.updated = false;
                this.navigator.onStart();
            }
            this.navigator.tick();
        } catch (RuntimeException e) {
            MessageUtils.sendErrorMessage(this.player, NavigatorCommand.KEY.then("error").translate(), e);
            CarpetOrgAddition.LOGGER.error("The navigator did not work as expected", e);
            // 清除导航器
            this.clearNavigator();
        }
    }

    @Nullable
    public AbstractNavigator getNavigator() {
        return this.navigator;
    }

    public void setNavigator(Entity entity, boolean isContinue) {
        this.setNavigator(new EntityNavigator(this.player, entity, isContinue));
    }

    public void setNavigator(Waypoint waypoint) {
        this.setNavigator(new WaypointNavigator(this.player, waypoint));
    }

    public void setNavigator(BlockPos blockPos, Level world) {
        this.setNavigator(new BlockPosNavigator(this.player, blockPos, world));
    }

    public void setNavigator(BlockPos blockPos, Level world, Component name) {
        this.setNavigator(new HasNamePosNavigator(this.player, blockPos, world, name));
    }

    private void setNavigator(@Nullable AbstractNavigator navigator) {
        ServerPlayNetworking.send(this.player, WaypointClearS2CPacket.INSTANCE);
        this.navigator = navigator;
        this.updated = true;
    }

    public void clearNavigator() {
        this.setNavigator((AbstractNavigator) null);
        ServerPlayNetworking.send(this.player, WaypointClearS2CPacket.INSTANCE);
    }

    public void setNavigatorFromOldPlayer(ServerPlayer oldPlayer) {
        NavigatorManager manager = PlayerComponentCoordinator.getCoordinator(oldPlayer).getNavigatorManager();
        AbstractNavigator navigator = manager.getNavigator();
        this.navigator = navigator == null ? null : navigator.copy(this.player);
    }
}
