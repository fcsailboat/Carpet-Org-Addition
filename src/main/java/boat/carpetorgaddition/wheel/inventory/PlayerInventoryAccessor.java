package boat.carpetorgaddition.wheel.inventory;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.wheel.screen.OfflinePlayerInventoryScreenHandler;
import boat.carpetorgaddition.wheel.screen.PlayerEnderChestScreenHandler;
import boat.carpetorgaddition.wheel.screen.PlayerInventoryScreenHandler;
import boat.carpetorgaddition.wheel.screen.WithButtonPlayerInventoryScreenHandler;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import boat.carpetorgaddition.wheel.text.TextJoiner;
import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuConstructor;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class PlayerInventoryAccessor {
    private final MenuConstructor inventory;
    private final MenuConstructor enderChest;
    private final Component displayName;
    private final GameProfile gameProfile;

    public PlayerInventoryAccessor(ServerPlayer interviewee, ServerPlayer visitor) {
        this.displayName = new TextJoiner()
                .append(TextBuilder.ofPlayerAvatar(interviewee).setColor(ChatFormatting.WHITE).build())
                .append(" ")
                .append(interviewee.getDisplayName())
                .join();
        this.gameProfile = interviewee.getGameProfile();
        this.inventory = (containerId, inventory, _) -> {
            if (CarpetOrgAdditionSettings.playerCommandOpenPlayerInventoryGcaStyle.value()) {
                return new WithButtonPlayerInventoryScreenHandler(containerId, interviewee, visitor);
            } else {
                return new PlayerInventoryScreenHandler(containerId, inventory, interviewee);
            }
        };
        this.enderChest = (containerId, inventory, _) -> new PlayerEnderChestScreenHandler(containerId, inventory, interviewee);
    }

    public PlayerInventoryAccessor(MinecraftServer server, GameProfile gameProfile, ServerPlayer visitor) {
        this.gameProfile = gameProfile;
        ServerPlayer interviewee = server.getPlayerList().getPlayer(gameProfile.id());
        if (interviewee == null) {
            FabricPlayerAccessManager accessManager = ServerComponentCoordinator.getCoordinator(server).getAccessManager();
            FabricPlayerAccessor accessor = accessManager.getOrCreate(gameProfile);
            Component name = LocalizationKeys.Operation.OFFLINE_PLAYER_NAME.translate(gameProfile.name());
            this.displayName = new TextJoiner()
                    .append(TextBuilder.ofPlayerAvatar(gameProfile.id()).setColor(ChatFormatting.WHITE).build())
                    .append(" ")
                    .append(name)
                    .join();
            this.inventory = (containerId, inventory, _) -> new OfflinePlayerInventoryScreenHandler(containerId, inventory, new OfflinePlayerInventory(accessor));
            this.enderChest = (containerId, inventory, _) -> ChestMenu.threeRows(containerId, inventory, new OfflinePlayerEnderChestInventory(accessor));
        } else {
            this.displayName = new TextJoiner()
                    .append(TextBuilder.ofPlayerAvatar(interviewee).setColor(ChatFormatting.WHITE).build())
                    .append(" ")
                    .append(interviewee.getDisplayName())
                    .join();
            this.inventory = (containerId, inventory, _) -> {
                if (CarpetOrgAdditionSettings.playerCommandOpenPlayerInventoryGcaStyle.value()) {
                    return new WithButtonPlayerInventoryScreenHandler(containerId, interviewee, visitor);
                } else {
                    return new PlayerInventoryScreenHandler(containerId, inventory, interviewee);
                }
            };
            this.enderChest = (containerId, inventory, _) -> new PlayerEnderChestScreenHandler(containerId, inventory, interviewee);
        }
    }

    public Component getDisplayName() {
        return this.displayName;
    }

    public GameProfile getGameProfile() {
        return this.gameProfile;
    }

    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player, PlayerInventoryType type) {
        AbstractContainerMenu menu = switch (type) {
            case INVENTORY -> this.inventory.createMenu(containerId, inventory, player);
            case ENDER_CHEST -> this.enderChest.createMenu(containerId, inventory, player);
        };
        if (menu == null) {
            throw new IllegalStateException("Failed to create container menu");
        }
        return menu;
    }
}
