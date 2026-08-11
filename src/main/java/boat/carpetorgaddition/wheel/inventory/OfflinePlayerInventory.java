package boat.carpetorgaddition.wheel.inventory;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.util.PlayerUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.NullMarked;

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
