package boat.carpetorgaddition.periodic.task.schedule;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.command.PlayerManagerCommand;
import boat.carpetorgaddition.exception.TaskExecutionException;
import boat.carpetorgaddition.periodic.fakeplayer.FakePlayerSerializer;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.PlayerUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.FakePlayerGameExitMarker;
import boat.carpetorgaddition.wheel.FakePlayerSpawner;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import carpet.patches.EntityPlayerMPFake;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Contract;

import java.util.UUID;

public class ReLoginTask extends PlayerScheduleTask {
    // 假玩家名
    private final FakePlayerSerializer serializer;
    // 重新上线的时间间隔
    private int interval;
    // 距离下一次重新上线所需的时间
    private int remainingTick;
    private final MinecraftServer server;
    private final CommandSourceStack source;
    // 当前任务是否已经结束
    private boolean stop = false;
    // 假玩家重新上线的倒计时
    private int canSpawn = 2;
    private final FakePlayerSpawner spawner;
    private final UUID uuid;
    public static final LocalizationKey KEY = PlayerManagerCommand.SCHEDULE.then("relogin");

    public ReLoginTask(EntityPlayerMPFake fakePlayer, int interval, MinecraftServer server, CommandSourceStack source) {
        super(source);
        this.serializer = new FakePlayerSerializer(fakePlayer);
        this.interval = interval;
        this.remainingTick = this.interval;
        this.server = server;
        this.source = source;
        this.spawner = this.serializer.getSpawner(this.server).setSilence(true).setPosition(null);
        this.uuid = fakePlayer.getUUID();
    }

    @Override
    public void tick() {
        // 启用内存泄漏修复
        if (CarpetOrgAdditionSettings.FAKE_PLAYER_SPAWN_MEMORY_LEAK_FIX.value()) {
            ServerPlayer player = this.server.getPlayerList().getPlayerByName(this.getPlayerName());
            if (player == null) {
                if (this.canSpawn <= 0) {
                    this.loginPlayer();
                    this.canSpawn = 2;
                } else {
                    this.canSpawn--;
                }
            } else if (this.remainingTick <= 0) {
                this.remainingTick = this.interval;
                if (player instanceof EntityPlayerMPFake fakePlayer) {
                    // 如果假玩家坠入虚空，设置任务为停止
                    ServerLevel world = ServerUtils.getWorld(fakePlayer);
                    if (fakePlayer.getY() < ServerUtils.getMinArchitectureAltitude(world) - 64) {
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
                MessageUtils.sendErrorMessage(this.source, KEY.then("rule_not_enabled").translate());
                // 如果假玩家已经下线，重新生成假玩家
                // TODO 游戏快进时，结束任务有时玩家不会重新上线
                ServerPlayer player = this.server.getPlayerList().getPlayerByName(this.getPlayerName());
                if (player == null) {
                    this.loginPlayer();
                }
            };
            throw new TaskExecutionException(function);
        }
    }

    /**
     * 让假玩家退出游戏
     */
    private void logoutPlayer(EntityPlayerMPFake fakePlayer) {
        FakePlayerGameExitMarker marker = (FakePlayerGameExitMarker) fakePlayer;
        if (marker.carpet_Org_Addition$isExitingTheGame()) {
            return;
        }
        marker.carpet_Org_Addition$markExitingTheGame();
        PlayerUtils.exitGameSilently(fakePlayer);
    }

    @Override
    public boolean stopped() {
        return this.stop;
    }

    @Override
    @Contract(pure = true)
    public String getPlayerName() {
        return this.serializer.getName();
    }

    public UUID getUuid() {
        return this.uuid;
    }

    @Override
    public void onCancel(CommandContext<CommandSourceStack> context) {
        this.markRemove();
        MessageUtils.sendMessage(context, KEY.then("stop").translate(this.getPlayerName()));
        ServerPlayer player = this.server.getPlayerList().getPlayerByName(this.getPlayerName());
        if (player == null) {
            this.loginPlayer();
        }
    }

    @Override
    public void sendEachMessage(CommandSourceStack source) {
        MessageUtils.sendMessage(source, KEY.translate(this.getPlayerName(), this.interval));
    }

    public void setInterval(int interval) {
        this.interval = interval;
        this.remainingTick = interval;
    }

    public void stop() {
        this.stop = true;
    }

    /**
     * 生成假玩家
     */
    private void loginPlayer() {
        try {
            this.spawner.spawn();
        } catch (RuntimeException e) {
            CarpetOrgAddition.LOGGER.warn("Fake player encounter unexpected errors while logging in", e);
            this.stop();
        }
    }
}
