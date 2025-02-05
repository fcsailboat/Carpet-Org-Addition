package org.carpetorgaddition.util;

import carpet.patches.EntityPlayerMPFake;
import carpet.patches.FakeClientConnection;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.UserCache;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.carpetorgaddition.mixin.rule.EntityAccessor;
import org.carpetorgaddition.mixin.rule.PlayerEntityAccessor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class GameUtils {
    private GameUtils() {
    }

    /**
     * 获取一名玩家的字符串形式的玩家名
     *
     * @param player 要获取字符串形式玩家名的玩家
     * @return 玩家名的字符串形式
     */
    public static String getPlayerName(PlayerEntity player) {
        return player.getName().getString();
    }

    /**
     * 获取当前系统时间的字符串形式
     *
     * @return 当前系统时间的字符串形式
     * @deprecated 不能使用资源包翻译
     */
    @Deprecated
    public static String getDateString() {
        LocalDateTime localDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss");
        return formatter.format(localDateTime);
    }

    @SuppressWarnings("DataFlowIssue")
    public static EntityPlayerMPFake createFakePlayer(String username, MinecraftServer server, Vec3d pos, double yaw, double pitch, RegistryKey<World> dimensionId, GameMode gamemode, boolean flying) {
        ServerWorld worldIn = server.getWorld(dimensionId);
        UserCache.setUseRemote(false);
        GameProfile gameprofile;
        try {
            gameprofile = server.getUserCache().findByName(username).orElse(null);
        } finally {
            UserCache.setUseRemote(server.isDedicated() && server.isOnlineMode());
        }
        if (gameprofile == null) {
            gameprofile = new GameProfile(Uuids.getOfflinePlayerUuid(username), username);
        }
        return trySpawn(server, pos, (float) yaw, (float) pitch, dimensionId, gamemode, flying, gameprofile, worldIn);
    }

    private static EntityPlayerMPFake trySpawn(MinecraftServer server, Vec3d pos, float yaw, float pitch, RegistryKey<World> dimensionId, GameMode gamemode, boolean flying, GameProfile gameprofile, ServerWorld worldIn) {
        // 生成假玩家的逻辑似乎不在主线程
        EntityPlayerMPFake fakePlayer = EntityPlayerMPFake.respawnFake(server, worldIn, gameprofile, SyncedClientOptions.createDefault());
        fakePlayer.fixStartingPosition = () -> fakePlayer.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, pitch);
        server.getPlayerManager().onPlayerConnect(new FakeClientConnection(NetworkSide.SERVERBOUND), fakePlayer, new ConnectedClientData(gameprofile, 0, fakePlayer.getClientOptions(), false));
        WorldUtils.teleport(fakePlayer, worldIn, pos.x, pos.y, pos.z, yaw, pitch);
        fakePlayer.setHealth(20.0F);
        ((EntityAccessor) fakePlayer).cancelRemoved();
        Objects.requireNonNull(fakePlayer.getAttributeInstance(EntityAttributes.STEP_HEIGHT)).setBaseValue(0.6F);
        fakePlayer.interactionManager.changeGameMode(gamemode);
        server.getPlayerManager().sendToDimension(new EntitySetHeadYawS2CPacket(fakePlayer, (byte) ((int) (fakePlayer.headYaw * 256.0F / 360.0F))), dimensionId);
        server.getPlayerManager().sendToDimension(EntityPositionSyncS2CPacket.create(fakePlayer), dimensionId);
        fakePlayer.getDataTracker().set(PlayerEntityAccessor.getPlayerModelParts(), (byte) 127);
        fakePlayer.getAbilities().flying = flying;
        return fakePlayer;
    }

    /**
     * @return 获取异常的类名+消息形式，如果没有消息，返回异常类的简单类名
     * @apiNote 不使用 {@code toString} 方法是因为方法可能被子类重写
     */
    public static String getExceptionString(Throwable throwable) {
        String name = throwable.getClass().getSimpleName();
        String message = throwable.getMessage();
        return message == null ? name : name + ": " + message;
    }

    /**
     * 一个占位符，什么也不做
     */
    public static void pass() {
    }
}
