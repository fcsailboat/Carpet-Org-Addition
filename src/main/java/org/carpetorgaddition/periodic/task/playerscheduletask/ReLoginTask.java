package org.carpetorgaddition.periodic.task.playerscheduletask;

import carpet.patches.EntityPlayerMPFake;
import carpet.patches.FakeClientConnection;
import carpet.utils.Messenger;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTask;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.UserCache;
import net.minecraft.util.Uuids;
import net.minecraft.world.World;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.exception.TaskExecutionException;
import org.carpetorgaddition.mixin.rule.EntityAccessor;
import org.carpetorgaddition.mixin.rule.PlayerEntityAccessor;
import org.carpetorgaddition.util.GameUtils;
import org.carpetorgaddition.util.MessageUtils;

import java.util.Objects;

public class ReLoginTask extends PlayerScheduleTask {
    // 假玩家名
    private final String playerName;
    // 重新上线的时间间隔
    private int interval;
    // 距离下一次重新上线所需的时间
    private int remainingTick;
    private final MinecraftServer server;
    private final RegistryKey<World> dimensionId;
    private final CommandContext<ServerCommandSource> context;
    // 当前任务是否已经结束
    private boolean stop = false;
    // 假玩家重新上线的倒计时
    private int canSpawn = 2;

    public ReLoginTask(
            String playerName,
            int interval,
            MinecraftServer server,
            RegistryKey<World> dimensionId,
            CommandContext<ServerCommandSource> context
    ) {
        this.playerName = playerName;
        this.interval = interval;
        this.remainingTick = this.interval;
        this.server = server;
        this.dimensionId = dimensionId;
        this.context = context;
    }

    @Override
    public void tick() {
        // 启用内存泄漏修复
        if (CarpetOrgAdditionSettings.fakePlayerSpawnMemoryLeakFix) {
            ServerPlayerEntity player = this.server.getPlayerManager().getPlayer(this.playerName);
            if (player == null) {
                if (this.canSpawn == 0) {
                    homePositionSpawn(this.playerName, this.server, this.dimensionId);
                    this.canSpawn = 2;
                } else {
                    this.canSpawn--;
                }
            } else if (this.remainingTick <= 0) {
                this.remainingTick = this.interval;
                if (player instanceof EntityPlayerMPFake fakePlayer) {
                    // 如果假玩家坠入虚空，设置任务为停止
                    if (fakePlayer.getY() < fakePlayer.getServerWorld().getBottomY() - 64) {
                        this.stop();
                    }
                    // 让假玩家退出游戏
                    this.logoutPlayer(fakePlayer);
                }
            } else {
                this.remainingTick--;
            }
        } else {
            Runnable function = () -> {
                MessageUtils.sendErrorMessage(context, "carpet.commands.playerManager.schedule.relogin.rule.disable");
                // 如果假玩家已经下线，重新生成假玩家
                ServerPlayerEntity player = this.server.getPlayerManager().getPlayer(this.playerName);
                if (player == null) {
                    homePositionSpawn(this.playerName, this.server, this.dimensionId);
                }
            };
            throw new TaskExecutionException(function);
        }
    }


    /**
     * 让假玩家退出游戏
     *
     * @see EntityPlayerMPFake#kill(Text)
     * @see EntityPlayerMPFake#shakeOff()
     */
    @SuppressWarnings("JavadocReference")
    private void logoutPlayer(EntityPlayerMPFake fakePlayer) {
        Text reason = Messenger.s("Killed");
        // 停止骑行
        if (fakePlayer.getVehicle() instanceof PlayerEntity) {
            fakePlayer.stopRiding();
        }
        for (Entity passenger : fakePlayer.getPassengersDeep()) {
            if (passenger instanceof PlayerEntity) {
                passenger.stopRiding();
            }
        }
        // 退出游戏
        TextContent content = reason.getContent();
        if (content instanceof TranslatableTextContent text) {
            if (text.getKey().equals("multiplayer.disconnect.duplicate_login")) {
                try {
                    CarpetOrgAdditionSettings.hiddenLoginMessages = true;
                    fakePlayer.networkHandler.onDisconnected(new DisconnectionInfo(reason));
                } finally {
                    CarpetOrgAdditionSettings.hiddenLoginMessages = false;
                }
                return;
            }
        }
        this.server.send(new ServerTask(this.server.getTicks(), () -> {
            try {
                CarpetOrgAdditionSettings.hiddenLoginMessages = true;
                /*
                 * 如果不加这个判断并提前返回，可能导致玩家的骑乘实体消失，可能的原因如下：
                 * 1. 玩家在下线后会保存一次数据，其中包括了当前骑乘的实体，下一次上线时，游戏就会从NBT中读取并生成骑乘实体。
                 * 2. 保存完自身的实体数据后就会从正在骑乘的实体身上下来，这时如果再获取这个玩家的骑乘实体就会返回null。
                 * 3. 接下来，如果这个玩家再触发一次保存，就会将null值写入玩家的骑乘实体，或者说，玩家就丢失了骑乘实体的数据。
                 * 4. 再次上线就无法重新生成之前的骑乘实体，因此，如果这个玩家已经被删除，就不能让该玩家再次触发保存了。
                 * 无法验证这样做是否能完全避免骑乘实体消失，也并不知道问题是不是真的由这个原因引起的，但这至少能保证在执行命令/tick sprint <time>时实体不会消失。
                 * 如果真的是因为不正确的玩家数据保存，那么保存是如何触发的，是服务器的自动保存吗？为什么执行/tick sprint <time>也会导致骑乘实体消失？
                 */
                if (fakePlayer.isRemoved()) {
                    return;
                }
                fakePlayer.networkHandler.onDisconnected(new DisconnectionInfo(reason));
            } finally {
                CarpetOrgAdditionSettings.hiddenLoginMessages = false;
            }
        }));
    }

    @Override
    public boolean stopped() {
        return this.stop;
    }

    @Override
    public String getLogName() {
        return this.playerName + "周期性重新上线";
    }

    @Override
    public String getPlayerName() {
        return playerName;
    }

    @Override
    public void onCancel(CommandContext<ServerCommandSource> context) {
        this.markRemove();
        MessageUtils.sendMessage(context, "carpet.commands.playerManager.schedule.relogin.cancel", this.playerName);
        ServerPlayerEntity player = this.server.getPlayerManager().getPlayer(this.playerName);
        if (player == null) {
            homePositionSpawn(this.playerName, this.server, this.dimensionId);
        }
    }

    @Override
    public void sendEachMessage(ServerCommandSource source) {
        MessageUtils.sendMessage(source, "carpet.commands.playerManager.schedule.relogin", this.playerName, this.interval);
    }

    public void setInterval(int interval) {
        this.interval = interval;
        this.remainingTick = interval;
    }

    public void stop() {
        this.stop = true;
    }

    /**
     * 在假玩家上一次退出游戏的位置生成假玩家
     *
     * @param username    假玩家名
     * @param dimensionId 假玩家要生成的维度
     */
    private void homePositionSpawn(String username, MinecraftServer server, RegistryKey<World> dimensionId) {
        ServerWorld worldIn = server.getWorld(dimensionId);
        if (worldIn == null) {
            return;
        }
        UserCache.setUseRemote(false);
        GameProfile gameprofile;
        try {
            UserCache userCache = server.getUserCache();
            if (userCache == null) {
                return;
            }
            gameprofile = userCache.findByName(username).orElse(null);
        } finally {
            UserCache.setUseRemote(server.isDedicated() && server.isOnlineMode());
        }
        if (gameprofile == null) {
            gameprofile = new GameProfile(Uuids.getOfflinePlayerUuid(username), username);
        }
        EntityPlayerMPFake fakePlayer = EntityPlayerMPFake.respawnFake(server, worldIn, gameprofile, SyncedClientOptions.createDefault());
        fakePlayer.fixStartingPosition = GameUtils::pass;
        try {
            CarpetOrgAdditionSettings.hiddenLoginMessages = true;
            server.getPlayerManager().onPlayerConnect(new FakeClientConnection(NetworkSide.SERVERBOUND), fakePlayer, new ConnectedClientData(gameprofile, 0, fakePlayer.getClientOptions(), false));
        } catch (NullPointerException e) {
            CarpetOrgAddition.LOGGER.warn("{}在尝试在服务器关闭时上线", this.playerName, e);
            this.stop();
            return;
        } finally {
            // 假玩家加入游戏后，这个变量必须重写设置为false，防止影响其它广播消息的方法
            CarpetOrgAdditionSettings.hiddenLoginMessages = false;
        }
        fakePlayer.setHealth(20.0F);
        ((EntityAccessor) fakePlayer).cancelRemoved();
        Objects.requireNonNull(fakePlayer.getAttributeInstance(EntityAttributes.GENERIC_STEP_HEIGHT)).setBaseValue(0.6F);
        server.getPlayerManager().sendToDimension(new EntitySetHeadYawS2CPacket(fakePlayer, (byte) ((int) (fakePlayer.headYaw * 256.0F / 360.0F))), dimensionId);
        server.getPlayerManager().sendToDimension(new EntityPositionS2CPacket(fakePlayer), dimensionId);
        fakePlayer.getDataTracker().set(PlayerEntityAccessor.getPlayerModelParts(), (byte) 127);
    }
}
