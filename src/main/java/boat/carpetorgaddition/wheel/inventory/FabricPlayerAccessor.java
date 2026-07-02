package boat.carpetorgaddition.wheel.inventory;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.mixin.accessor.PlayerManagerAccessor;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class FabricPlayerAccessor {
    private final FakePlayer fabricPlayer;
    private final MinecraftServer server;
    private final FabricPlayerAccessManager accessManager;
    /**
     * 正在查看物品栏的玩家数
     */
    private final Set<ServerPlayer> viewers = new HashSet<>();
    private final NameAndId entry;
    public static final ScopedValue<Boolean> INIT_FABRIC_PLAYER = ScopedValue.newInstance();

    public FabricPlayerAccessor(MinecraftServer server, NameAndId entry, FabricPlayerAccessManager accessManager) {
        this.server = server;
        this.fabricPlayer = FakePlayer.get(server.overworld(), new GameProfile(entry.id(), entry.name()));
        this.entry = entry;
        this.initFakePlayer(server);
        this.accessManager = accessManager;
        this.accessManager.addViewers(entry, this.viewers);
    }

    /**
     * @see PlayerList#placeNewPlayer(Connection, ServerPlayer, CommonListenerCookie)
     */
    @SuppressWarnings("deprecation")
    private void initFakePlayer(MinecraftServer server) {
        ProblemReporter.ScopedCollector logging = new ProblemReporter.ScopedCollector(CarpetOrgAddition.LOGGER);
        try (logging) {
            Optional<ValueInput> optional = server.getPlayerList()
                    .loadPlayerData(this.fabricPlayer.nameAndId())
                    .map(nbtCompound -> TagValueInput.create(logging, server.registryAccess(), nbtCompound));
            ServerPlayer.SavedPosition pos = optional
                    .flatMap(nbt -> nbt.read(ServerPlayer.SavedPosition.MAP_CODEC))
                    .orElse(ServerPlayer.SavedPosition.EMPTY);
            optional.ifPresent(input -> {
                this.fabricPlayer.load(input);
                ScopedValue.where(INIT_FABRIC_PLAYER, true).run(() -> this.fabricPlayer.loadAndSpawnParentVehicle(input));
            });
            ServerLevel world = server.getLevel(pos.dimension().orElse(Level.OVERWORLD));
            if (world != null) {
                // 设置玩家所在维度
                this.fabricPlayer.setServerLevel(world);
            }
        }
    }

    public Inventory getInventory() {
        return this.fabricPlayer.getInventory();
    }

    public PlayerEnderChestContainer getEnderChest() {
        return this.fabricPlayer.getEnderChestInventory();
    }

    public void onOpen(ServerPlayer player) {
        this.viewers.add(player);
    }

    public void onClose(ServerPlayer player) {
        this.viewers.remove(player);
        if (this.viewers.isEmpty()) {
            try {
                // 保存玩家数据
                PlayerList playerManager = this.server.getPlayerList();
                PlayerManagerAccessor accessor = (PlayerManagerAccessor) playerManager;
                accessor.savePlayerEntityData(this.fabricPlayer);
            } catch (RuntimeException e) {
                CarpetOrgAddition.LOGGER.error("Failed to save player data for {}", this.entry.name(), e);
            } finally {
                this.accessManager.removeAccessor(this.entry);
                this.accessManager.removeViewer(this.entry);
            }
        }
    }

    public NameAndId getPlayerConfigEntry() {
        return this.entry;
    }
}
