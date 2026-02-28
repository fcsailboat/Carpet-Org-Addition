package boat.carpetorgaddition.wheel.screen;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.util.MathUtils;
import boat.carpetorgaddition.wheel.inventory.PlayerStorageInventory;
import boat.carpetorgaddition.wheel.inventory.ServerPlayerInventory;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import org.jspecify.annotations.NonNull;

public class PlayerInventoryScreenHandler extends AbstractPlayerInventoryScreenHandler<ServerPlayerInventory> {
    private final ServerPlayer player;

    public PlayerInventoryScreenHandler(int syncId, Inventory inventory, ServerPlayer player) {
        super(syncId, inventory, new ServerPlayerInventory(player));
        this.player = player;
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        // 玩家活着，并且玩家没有被删除
        return !this.player.isDeadOrDying() && !this.player.isRemoved();
    }

    @Override
    public void clicked(int slotIndex, int button, @NonNull ContainerInput input, @NonNull Player player) {
        if (CarpetOrgAddition.isDebugMode() && MathUtils.isInRange(this.from(), this.to(), slotIndex) && this.player instanceof EntityPlayerMPFake fakePlayer) {
            PlayerStorageInventory inventory = new PlayerStorageInventory(fakePlayer);
            inventory.sort();
        }
        super.clicked(slotIndex, button, input, player);
    }

    @Override
    public void removed(@NonNull Player player) {
        super.removed(player);
        this.inventory.stopOpen(player);
    }
}
