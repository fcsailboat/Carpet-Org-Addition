package boat.carpetorgaddition.wheel;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.util.ServerUtils;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;

@NullMarked
public class FakePlayerSpawner {
    /**
     * 假玩家在上线或下线时，是否隐藏上下线的消息
     */
    public static final ScopedValue<Boolean> SILENCE = ScopedValue.newInstance();
    /**
     * 假玩家生成后执行的回调函数
     */
    public static final ScopedValue<Consumer<EntityPlayerMPFake>> CALLBACK = ScopedValue.newInstance();
    /**
     * 假玩家是否在上次下线的位置生成
     */
    public static final ScopedValue<Boolean> ORIGINAL_POSITION = ScopedValue.newInstance();
    /**
     * 假玩家的召唤者
     */
    public static final ScopedValue<Optional<ServerPlayer>> SUMMONER = ScopedValue.newInstance();
    /**
     * 玩家的名称
     */
    private final String name;
    private final MinecraftServer server;
    /**
     * 假玩家生成的位置
     */
    @Nullable
    private Vec3 position;
    /**
     * 假玩家生成的维度
     */
    private ResourceKey<Level> dimension;
    /**
     * 水平方向的朝向
     */
    private double yaw = 0.0;
    /**
     * 垂直方向的朝向
     */
    private double pitch = 0.0;
    /**
     * 游戏模式
     */
    private GameType gameMode;
    /**
     * 是否创造模式飞行
     */
    private boolean flying = false;
    /**
     * 是否潜行
     */
    private boolean sneaking = false;
    /**
     * 在假玩家生成后执行
     */
    private Consumer<EntityPlayerMPFake> callback = CarpetOrgAddition::pass;
    /**
     * 是否隐藏登录消息
     */
    private boolean silence;
    @Nullable
    private ServerPlayer summoner;

    private FakePlayerSpawner(MinecraftServer server, String name) {
        this.server = server;
        this.name = name;
        this.position = ServerUtils.getBlockBottomCenter(server.getRespawnData().pos());
        this.dimension = server.overworld().dimension();
        this.gameMode = server.getDefaultGameType();
    }

    public static FakePlayerSpawner of(MinecraftServer server, String name) {
        return new FakePlayerSpawner(server, name);
    }

    public FakePlayerSpawner setPosition(@Nullable Vec3 position) {
        this.position = position;
        return this;
    }

    public FakePlayerSpawner setGameMode(GameType gameMode) {
        this.gameMode = gameMode;
        return this;
    }

    public FakePlayerSpawner setWorld(Level world) {
        return this.setWorld(world.dimension());
    }

    public FakePlayerSpawner setWorld(ResourceKey<Level> world) {
        this.dimension = world;
        return this;
    }

    public FakePlayerSpawner setYaw(double yaw) {
        this.yaw = yaw;
        return this;
    }

    public FakePlayerSpawner setPitch(double pitch) {
        this.pitch = pitch;
        return this;
    }

    public FakePlayerSpawner setFlying(boolean flying) {
        this.flying = flying;
        return this;
    }

    public FakePlayerSpawner setSneaking(boolean sneaking) {
        this.sneaking = sneaking;
        return this;
    }

    public FakePlayerSpawner setCallback(@Nullable Consumer<EntityPlayerMPFake> callback) {
        this.callback = callback == null ? CarpetOrgAddition::pass : callback;
        return this;
    }

    public FakePlayerSpawner setSilence(boolean silence) {
        this.silence = silence;
        return this;
    }

    public FakePlayerSpawner setSummoner(@Nullable ServerPlayer summoner) {
        this.summoner = summoner;
        return this;
    }

    /**
     * 如果玩家不存在，则召唤玩家
     *
     * @return 是否召唤成功
     */
    public boolean spawn() {
        if (ServerUtils.getPlayer(server, this.name).isPresent()) {
            return false;
        }
        if (this.sneaking) {
            this.callback = this.callback.andThen(fakePlayer -> fakePlayer.setShiftKeyDown(true));
        }
        return ScopedValue.where(SILENCE, this.silence)
                .where(CALLBACK, this.callback)
                .where(ORIGINAL_POSITION, this.position == null)
                .where(SUMMONER, Optional.ofNullable(this.summoner))
                .call(() -> EntityPlayerMPFake.createFake(
                        this.name,
                        this.server,
                        this.position == null ? Vec3.ZERO : this.position,
                        this.yaw,
                        this.pitch,
                        this.dimension,
                        this.gameMode,
                        this.flying
                ));
    }
}
