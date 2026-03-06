package boat.carpetorgaddition.periodic;

import boat.carpetorgaddition.periodic.navigator.NavigatorManager;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.inventory.WithButtonPlayerInventory;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class PlayerComponentCoordinator {
    private final ServerPlayer player;
    protected final MinecraftServer server;
    private final NavigatorManager navigatorManager;
    private final WithButtonPlayerInventory withButtonPlayerInventory;
    @Nullable
    private GameType nbtGameMode;
    /**
     * 客户端和服务端的自定义单击事件数据版本不匹配，是否已发送通知
     */
    private boolean versionMismatchNotified = false;

    public PlayerComponentCoordinator(ServerPlayer player) {
        this.player = player;
        this.server = ServerUtils.getServer(player);
        this.navigatorManager = new NavigatorManager(player);
        this.withButtonPlayerInventory = new WithButtonPlayerInventory(player);
    }

    public static PlayerComponentCoordinator of(ServerPlayer player) {
        return switch (player) {
            case EntityPlayerMPFake fakePlayer -> new FakePlayerComponentCoordinator(fakePlayer);
            case ServerPlayer _ -> new PlayerComponentCoordinator(player);
            case null -> throw new NullPointerException("player may not be null");
        };
    }

    public void tick() {
        this.navigatorManager.tick();
        this.withButtonPlayerInventory.tick();
    }

    public NavigatorManager getNavigatorManager() {
        return this.navigatorManager;
    }

    public WithButtonPlayerInventory getWithButtonPlayerInventory() {
        return this.withButtonPlayerInventory;
    }

    public static PlayerComponentCoordinator getCoordinator(ServerPlayer player) {
        return ((PeriodicTaskManagerInterface) player).carpet_Org_Addition$getPlayerPeriodicTaskManager();
    }

    public static FakePlayerComponentCoordinator getCoordinator(EntityPlayerMPFake fakePlayer) {
        return (FakePlayerComponentCoordinator) getCoordinator((ServerPlayer) fakePlayer);
    }

    /**
     * <p>
     * <s>玩家通过末地返回传送门时，实际上是创建了一个新对象，然后将原有的数据拷贝到了新对象上，而本类的对象也是玩家的一个成员变量，因此也要进行拷贝。</s>
     * </p>
     * <p>
     * 在{@code 1.21}中通过调试发现这个方法并没有在玩家进入返回传送门时执行，传送逻辑已经被修改了吗？
     * </p>
     */
    public void copyFrom(ServerPlayer oldPlayer) {
        this.navigatorManager.setNavigatorFromOldPlayer(oldPlayer);
    }

    protected ServerPlayer getPlayer() {
        return this.player;
    }

    public boolean isVersionMismatchNotified() {
        return this.versionMismatchNotified;
    }

    public void markVersionMismatchNotified() {
        this.versionMismatchNotified = true;
    }

    @Nullable
    public GameType getNbtGameMode() {
        return this.nbtGameMode;
    }

    public void setNbtGameMode(GameType gameMode) {
        this.nbtGameMode = gameMode;
    }
}
