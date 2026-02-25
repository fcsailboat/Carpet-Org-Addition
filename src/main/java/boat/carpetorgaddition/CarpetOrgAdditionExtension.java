package boat.carpetorgaddition;

import boat.carpetorgaddition.command.CommandRegister;
import boat.carpetorgaddition.command.PlayerManagerCommand;
import boat.carpetorgaddition.command.SpectatorCommand;
import boat.carpetorgaddition.config.GlobalConfigs;
import boat.carpetorgaddition.logger.LoggerRegister;
import boat.carpetorgaddition.network.s2c.PlayerTypeSyncS2CPacket;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.periodic.parcel.ParcelManager;
import boat.carpetorgaddition.periodic.task.search.OfflinePlayerSearchTask;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.FakePlayerSpawner;
import boat.carpetorgaddition.wheel.GameProfileCache;
import boat.carpetorgaddition.wheel.permission.PermissionManager;
import boat.carpetorgaddition.wheel.text.Translation;
import carpet.CarpetExtension;
import carpet.CarpetServer;
import carpet.api.settings.SettingsManager;
import carpet.patches.EntityPlayerMPFake;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

public class CarpetOrgAdditionExtension implements CarpetExtension {
    private static boolean settingsLoaded = false;

    public CarpetOrgAdditionExtension() {
    }

    // 在游戏开始时
    @Override
    public void onGameStarted() {
        // 解析Carpet设置
        CarpetOrgAdditionSettings.register();
        settingsLoaded = true;
    }

    public static SettingsManager getSettingManager() {
        return CarpetServer.settingsManager;
    }

    public static boolean isCarpetRuleLoaded() {
        return settingsLoaded;
    }

    // 当玩家登录时
    @Override
    public void onPlayerLoggedIn(ServerPlayer player) {
        if (CarpetOrgAddition.isDebugDevelopment() && !FakePlayerSpawner.SILENCE.orElse(false)) {
            GameProfile gameProfile = player.getGameProfile();
            CarpetOrgAddition.LOGGER.info(new NameAndId(gameProfile).toString());
        }
        // 假玩家生成时不保留上一次的击退，着火时间，摔落高度
        clearKnockback(player);
        // 提示玩家接收快递
        MinecraftServer server = ServerUtils.getServer(player);
        ServerComponentCoordinator coordinator = ServerComponentCoordinator.getCoordinator(server);
        ParcelManager parcelManager = coordinator.getParcelManager();
        parcelManager.promptToCollect(player);
        // 加载假玩家安全挂机
        PlayerManagerCommand.loadSafeAfk(player);
        this.teleportSpectatorPlayer(player, server);
        switch (player) {
            case EntityPlayerMPFake fakePlayer -> {
                coordinator.getSavedFakePlayer().put(fakePlayer);
                for (ServerPlayer realPlayer : server.getPlayerList().getPlayers()) {
                    if (realPlayer instanceof EntityPlayerMPFake) {
                        continue;
                    }
                    ServerPlayNetworking.send(realPlayer, new PlayerTypeSyncS2CPacket(player.getUUID(), true));
                }
            }
            case ServerPlayer _ -> {
                for (ServerPlayer fakePlayer : server.getPlayerList().getPlayers()) {
                    if (fakePlayer instanceof EntityPlayerMPFake) {
                        ServerPlayNetworking.send(player, new PlayerTypeSyncS2CPacket(fakePlayer.getUUID(), true));
                    }
                }
            }
        }
    }

    /**
     * 如果{@code server.properties}中，{@code force-gamemode}为{@code true}，则将被{@code /spectator}命令设置为旁观模式的玩家传送回原位置
     */
    private void teleportSpectatorPlayer(ServerPlayer player, MinecraftServer server) {
        GameType gameMode = server.getForcedGameType();
        if (gameMode == null) {
            return;
        }
        SpectatorCommand instance = CommandRegister.getCommandInstance(SpectatorCommand.class);
        try {
            instance.loadAndTeleportPlayer(server, player);
        } catch (RuntimeException e) {
            CarpetOrgAddition.LOGGER.error("Unexpected error while attempting to teleport player back to original location", e);
        }
    }

    @Override
    public void onPlayerLoggedOut(ServerPlayer player) {
        MinecraftServer server = ServerUtils.getServer(player);
        ServerComponentCoordinator coordinator = ServerComponentCoordinator.getCoordinator(server);
        if (player instanceof EntityPlayerMPFake fakePlayer) {
            coordinator.getSavedFakePlayer().remove(fakePlayer);
            server.getPlayerList().getPlayers().stream()
                    .filter(realPlayer -> !(realPlayer instanceof EntityPlayerMPFake))
                    .forEach(realPlayer -> ServerPlayNetworking.send(realPlayer, new PlayerTypeSyncS2CPacket(player.getUUID(), false)));
        }
    }

    /**
     * 清除击退效果
     */
    private static void clearKnockback(ServerPlayer player) {
        if (CarpetOrgAdditionSettings.fakePlayerSpawnNoKnockback.value() && player instanceof EntityPlayerMPFake) {
            // 清除速度
            player.setDeltaMovement(Vec3.ZERO);
            // 清除着火时间
            player.setRemainingFireTicks(0);
            // 清除摔落高度
            player.fallDistance = 0;
            // 清除负面效果
            player.getActiveEffects().removeIf(effect -> effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL);
        }
    }

    @Override
    public void onServerLoadedWorlds(MinecraftServer server) {
        ServerComponentCoordinator.getCoordinator(server).onServerStarted();
        PermissionManager.load(server);
    }

    @Override
    public void onServerClosed(MinecraftServer server) {
        GameProfileCache.getInstance().save();
        PermissionManager.reset();
        GlobalConfigs.getInstance().save();
        OfflinePlayerSearchTask.clear();
    }

    // 设置模组翻译
    @Override
    public Map<String, String> canHasTranslations(String lang) {
        return Translation.getInstance().getTranslation();
    }

    // 注册记录器
    @Override
    public void registerLoggers() {
        LoggerRegister.register();
    }

    // 注册命令
    @Override
    public void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext access) {
        CommandRegister.register(dispatcher, access);
    }
}
