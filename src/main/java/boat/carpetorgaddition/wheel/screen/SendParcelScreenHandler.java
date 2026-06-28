package boat.carpetorgaddition.wheel.screen;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.command.MailCommand;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.periodic.parcel.Parcel;
import boat.carpetorgaddition.periodic.parcel.ParcelManager;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.inventory.AutoGrowInventory;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import com.mojang.authlib.GameProfile;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import org.jspecify.annotations.NonNull;

import java.io.IOException;

public class SendParcelScreenHandler extends ChestMenu {
    private final Container inventory;
    private final ParcelManager parcelManager;
    private final MinecraftServer server;
    private final ServerPlayer sourcePlayer;
    private final GameProfile recipient;
    private static final LocalizationKey SEND = MailCommand.SEND.then("multiple");

    public SendParcelScreenHandler(
            int syncId,
            Inventory playerInventory,
            ServerPlayer sourcePlayer,
            GameProfile recipient,
            Container inventory
    ) {
        super(MenuType.GENERIC_9x3, syncId, playerInventory, inventory, 3);
        this.inventory = inventory;
        this.server = ServerUtils.getServer(sourcePlayer);
        this.parcelManager = ServerComponentCoordinator.of(this.server).getParcelManager();
        this.sourcePlayer = sourcePlayer;
        this.recipient = recipient;
    }

    @Override
    public void removed(@NonNull Player player) {
        super.removed(player);
        if (this.inventory.isEmpty()) {
            return;
        }
        AutoGrowInventory autoGrowInventory = new AutoGrowInventory();
        // 合并可堆叠的物品
        this.inventory.forEach(autoGrowInventory::addStack);
        // 发送物品
        Parcel parcel = new Parcel(this.server, this.sourcePlayer, this.recipient, autoGrowInventory, this.parcelManager.generateNumber());
        try {
            parcelManager.put(parcel);
        } catch (IOException e) {
            CarpetOrgAddition.LOGGER.error("Encountered an unexpected error while batch sending items", e);
            CommandSourceStack source = this.sourcePlayer.createCommandSourceStack();
            MessageUtils.sendErrorMessage(source, SEND.then("error").translate(), e);
        }
    }
}
