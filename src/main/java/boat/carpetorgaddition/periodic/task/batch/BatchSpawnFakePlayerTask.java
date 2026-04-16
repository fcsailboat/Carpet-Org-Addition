package boat.carpetorgaddition.periodic.task.batch;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.command.PlayerManagerCommand;
import boat.carpetorgaddition.periodic.task.ServerTask;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.PlayerUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.FakePlayerSpawner;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserNameToIdResolver;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class BatchSpawnFakePlayerTask extends ServerTask {
    /**
     * 最大预加载时间，超时则抛出异常中止任务
     */
    public static final long MAX_PRELOADING_TIME = 60 * 1000L;
    /**
     * 是否正在预加载玩家档案，用于抑制{@code Couldn't find profile with name: {}}警告
     */
    public static final ScopedValue<Boolean> REQUEST = ScopedValue.newInstance();
    /**
     * 所有要召唤的玩家
     */
    private final Set<FakePlayerSpawner> spawners = ConcurrentHashMap.newKeySet();
    private final MinecraftServer server;
    /**
     * 要召唤的玩家数量
     */
    private final int count;
    /**
     * 上一个游戏刻已经召唤的玩家数量
     */
    private int prevCount = 0;
    /**
     * 是否正在预加载
     */
    private boolean preload = true;
    private Iterator<FakePlayerSpawner> iterator;
    /**
     * 是否召唤完成，用于确定任务是否结束
     */
    private boolean complete = false;
    /**
     * 任务的开始时间
     */
    private final long startTime;

    public BatchSpawnFakePlayerTask(MinecraftServer server, CommandSourceStack source, Function<String, FakePlayerSpawner> spawner, List<String> names) {
        super(source);
        this.server = server;
        UserNameToIdResolver cache = server.services().nameToIdCache();
        List<String> list = names.stream()
                .filter(PlayerUtils::verifyNameLength)
                .filter(name -> ServerUtils.getPlayer(server, name).isEmpty())
                .toList();
        for (String name : list) {
            Thread.ofVirtual().start(() -> {
                Optional<NameAndId> optional = ScopedValue.where(REQUEST, true).call(() -> cache.get(name));
                NameAndId gameProfile = optional.orElseGet(() -> new NameAndId(UUIDUtil.createOfflinePlayerUUID(name), name));
                if (optional.isEmpty()) {
                    cache.add(gameProfile);
                }
                this.spawners.add(spawner.apply(gameProfile.name()));
            });
        }
        this.count = list.size();
        this.startTime = ServerUtils.getWorld(this.source).getGameTime();
    }

    @Override
    protected void tick() {
        if (this.count == 0) {
            MessageUtils.sendErrorMessage(this.source, PlayerManagerCommand.BATCH.then("not_summoned").translate());
            this.complete = true;
            return;
        }
        int size = this.spawners.size();
        long time = ServerUtils.getWorld(this.source).getGameTime();
        if (this.preload) {
            if (this.getElapsedTime() > MAX_PRELOADING_TIME) {
                this.throwTimeoutException();
            }
            // 任务开始前几个游戏刻不显示进度
            boolean progress = time - this.startTime > 10;
            LocalizationKey key = PlayerManagerCommand.BATCH.then("preload");
            if (size < this.count) {
                if (progress && (this.prevCount != size || time % 40 == 0)) {
                    this.prevCount = size;
                    MessageUtils.sendMessageToHudIfPlayer(this.source, () -> key.translate(size, this.count));
                }
                return;
            }
            if (progress) {
                MessageUtils.sendMessageToHudIfPlayer(this.source, () -> key.then("done").translate());
            }
            this.preload = false;
            this.setStartTime();
        }
        this.checkTimeout();
        if (this.iterator == null) {
            this.iterator = this.spawners.iterator();
        }
        while (this.iterator.hasNext()) {
            if (this.isTimeExpired()) {
                return;
            }
            this.iterator.next().spawn();
        }
        PlayerManagerCommand.sendPlayerJoinMessage(this.server, this.count);
        // 显示玩家召唤者
        if (CarpetOrgAdditionSettings.DISPLAY_PLAYER_SUMMONER.value()) {
            Component summoner = LocalizationKeys.Rule.Message.DISPLAY_PLAYER_SUMMONER
                    .builder(this.source.getDisplayName())
                    .setGrayItalic()
                    .build();
            MessageUtils.sendMessage(this.server, summoner);
        }
        this.complete = true;
    }

    @Override
    protected long getMaxExecutionTime() {
        return 5000L;
    }

    @Override
    protected long getMaxTimeSlice() {
        return 50L;
    }

    @Override
    protected boolean stopped() {
        return this.complete;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && (this == obj || this.getClass() == obj.getClass());
    }

    @Override
    public int hashCode() {
        return 1;
    }
}
