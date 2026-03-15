package boat.carpetorgaddition.periodic.task.schedule;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.command.PlayerManagerCommand;
import boat.carpetorgaddition.exception.TaskExecutionException;
import boat.carpetorgaddition.periodic.fakeplayer.FakePlayerSerializer;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.FakePlayerSpawner;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import carpet.patches.EntityPlayerMPFake;
import carpet.utils.Messenger;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Contract;

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
    public static final LocalizationKey KEY = PlayerManagerCommand.SCHEDULE.then("relogin");

    public ReLoginTask(EntityPlayerMPFake fakePlayer, int interval, MinecraftServer server, CommandSourceStack source) {
        super(source);
        this.serializer = new FakePlayerSerializer(fakePlayer);
        this.interval = interval;
        this.remainingTick = this.interval;
        this.server = server;
        this.source = source;
        this.spawner = this.serializer.getSpawner(this.server).setSilence(true).setPosition(null);
    }

    @Override
    public void tick() {
        // 启用内存泄漏修复
        if (CarpetOrgAdditionSettings.FAKE_PLAYER_SPAWN_MEMORY_LEAK_FIX.value()) {
            ServerPlayer player = this.server.getPlayerList().getPlayerByName(this.getPlayerName());
            if (player == null) {
                if (this.canSpawn == 0) {
                    loginPlayer();
                    this.canSpawn = 2;
                } else {
                    this.canSpawn--;
                }
            } else if (this.remainingTick <= 0) {
                this.remainingTick = this.interval;
                if (player instanceof EntityPlayerMPFake fakePlayer) {
                    // 如果假玩家坠入虚空，设置任务为停止
                    if (fakePlayer.getY() < ServerUtils.getWorld(fakePlayer).getMinY() - 64) {
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
                ServerPlayer player = this.server.getPlayerList().getPlayerByName(this.getPlayerName());
                if (player == null) {
                    loginPlayer();
                }
            };
            throw new TaskExecutionException(function);
        }
    }

    /**
     * 让假玩家退出游戏
     *
     * @see EntityPlayerMPFake#kill(Component)
     * @see EntityPlayerMPFake#shakeOff()
     */
    @SuppressWarnings("JavadocReference")
    private void logoutPlayer(EntityPlayerMPFake fakePlayer) {
        Component reason = Messenger.s("Killed");
        // 停止骑行
        if (fakePlayer.getVehicle() instanceof Player) {
            fakePlayer.stopRiding();
        }
        for (Entity passenger : fakePlayer.getIndirectPassengers()) {
            if (passenger instanceof Player) {
                passenger.stopRiding();
            }
        }
        // 退出游戏
        ComponentContents content = reason.getContents();
        if (content instanceof TranslatableContents text) {
            if (text.getKey().equals("multiplayer.disconnect.duplicate_login")) {
                ScopedValue.where(FakePlayerSpawner.SILENCE, true)
                        .run(() -> fakePlayer.connection.onDisconnect(new DisconnectionDetails(reason)));
                return;
            }
        }
        MinecraftServer server = ServerUtils.getServer(fakePlayer);
        server.schedule(new TickTask(server.getTickCount(),
                () -> ScopedValue.where(FakePlayerSpawner.SILENCE, true).run(() -> {
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
                    fakePlayer.connection.onDisconnect(new DisconnectionDetails(reason));
                })));
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

    @Override
    public void onCancel(CommandContext<CommandSourceStack> context) {
        this.markRemove();
        MessageUtils.sendMessage(context, KEY.then("stop").translate(this.getPlayerName()));
        ServerPlayer player = this.server.getPlayerList().getPlayerByName(this.getPlayerName());
        if (player == null) {
            loginPlayer();
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
