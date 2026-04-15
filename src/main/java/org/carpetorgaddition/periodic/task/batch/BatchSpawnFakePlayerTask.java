package org.carpetorgaddition.periodic.task.batch;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.UserCache;
import net.minecraft.util.Uuids;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.periodic.task.ServerTask;
import org.carpetorgaddition.util.FetcherUtils;
import org.carpetorgaddition.util.GenericUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.wheel.FakePlayerCreateContext;
import org.carpetorgaddition.wheel.TextBuilder;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BatchSpawnFakePlayerTask extends ServerTask {
    public static final ThreadLocal<Boolean> batchSpawnHiddenMessage = ThreadLocal.withInitial(() -> false);
    public static final ThreadLocal<Boolean> internalBatchSpawnHiddenMessage = ThreadLocal.withInitial(() -> false);
    /**
     * 所有要召唤的玩家
     */
    private final Set<GameProfile> players = ConcurrentHashMap.newKeySet();
    private final MinecraftServer server;
    /**
     * 玩家名称的前缀
     */
    private final String prefix;
    private final int start;
    private final int end;
    /**
     * 玩家的创建上下文，用于确定玩家上线的位置，维度，朝向等
     */
    private final FakePlayerCreateContext context;
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
    private boolean isPreload = true;
    private Iterator<GameProfile> iterator;
    /**
     * 是否召唤完成，用于确定任务是否结束
     */
    private boolean complete = false;
    /**
     * 任务的开始时间
     */
    private final long startTime;
    /**
     * 玩家档案预加载使用的时间
     */
    private long setupTime = -1L;
    private final long startSystemTime = System.currentTimeMillis();

    public BatchSpawnFakePlayerTask(MinecraftServer server, ServerCommandSource source, UserCache userCache, FakePlayerCreateContext context, String prefix, int start, int end) {
        super(source);
        this.server = server;
        this.prefix = prefix;
        this.start = start;
        this.end = end;
        int count = end - start + 1;
        this.context = context;
        PlayerManager playerManager = server.getPlayerManager();
        for (int i = start; i <= end; i++) {
            String username = prefix + i;
            if (playerManager.getPlayer(username) != null) {
                count--;
                continue;
            }
            Thread.ofVirtual().start(() -> {
                Optional<GameProfile> optional = userCache.findByName(username);
                GameProfile gameProfile = optional.orElseGet(() -> new GameProfile(Uuids.getOfflinePlayerUuid(username), username));
                if (optional.isEmpty()) {
                    userCache.add(gameProfile);
                }
                this.players.add(gameProfile);
            });
        }
        this.count = count;
        this.startTime = FetcherUtils.getWorld(this.source).getTime();
    }

    @Override
    protected void tick() {
        int size = this.players.size();
        long time = FetcherUtils.getWorld(this.source).getTime();
        if (this.isPreload) {
            if (System.currentTimeMillis() - this.startSystemTime > 60 * 1000L) {
                this.timeout();
            }
            // 任务开始前几个游戏刻不显示进度
            boolean progress = time - this.startTime > 10;
            if (size < this.count) {
                if (progress && (this.prevCount != size || time % 40 == 0)) {
                    this.prevCount = size;
                    Text message = TextBuilder.translate("carpet.commands.playerManager.batch.preload", size, this.count);
                    MessageUtils.sendMessageToHudIfPlayer(this.source, message);
                }
                return;
            }
            if (progress) {
                Text message = TextBuilder.translate("carpet.commands.playerManager.batch.preload.done");
                MessageUtils.sendMessageToHudIfPlayer(this.source, message);
            }
            this.isPreload = false;
        }
        if (this.setupTime == -1L) {
            this.setupTime = this.getExecutionTime();
        }
        this.checkTimeout();
        try {
            batchSpawnHiddenMessage.set(true);
            if (this.iterator == null) {
                this.iterator = this.players.iterator();
            }
            while (this.iterator.hasNext()) {
                if (this.isTimeExpired()) {
                    return;
                }
                GameProfile gameProfile = iterator.next();
                GenericUtils.createFakePlayer(gameProfile.getName(), this.server, this.context);
            }
        } finally {
            batchSpawnHiddenMessage.set(false);
        }
        // 显示玩家召唤者
        if (CarpetOrgAdditionSettings.displayPlayerSummoner.get()) {
            Text summoner = TextBuilder.translate(
                    "carpet.commands.playerManager.batch.summoner",
                    this.source.getDisplayName(),
                    this.prefix + this.start,
                    this.prefix + this.end,
                    this.count
            );
            MessageUtils.broadcastMessage(this.server, summoner);
        }
        this.complete = true;
    }

    @Override
    protected long getMaxExecutionTime() {
        return 5000L + this.setupTime;
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
    public String getLogName() {
        return "玩家批量生成";
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
