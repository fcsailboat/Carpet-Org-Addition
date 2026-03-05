package org.carpetorgaddition.periodic;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import org.carpetorgaddition.periodic.navigator.NavigatorManager;
import org.jetbrains.annotations.NotNull;

public class PlayerComponentCoordinator {
    private final ServerPlayerEntity player;
    private final NavigatorManager navigatorManager;
    private GameMode nbtGameMode;

    public PlayerComponentCoordinator(ServerPlayerEntity player) {
        this.player = player;
        this.navigatorManager = new NavigatorManager(player);
    }

    public static PlayerComponentCoordinator of(ServerPlayerEntity serverPlayerEntity) {
        return switch (serverPlayerEntity) {
            case EntityPlayerMPFake fakePlayer -> new FakePlayerComponentCoordinator(fakePlayer);
            case ServerPlayerEntity player -> new PlayerComponentCoordinator(player);
            case null -> throw new NullPointerException("player may not be null");
        };
    }

    public void tick() {
        this.navigatorManager.tick();
    }

    public NavigatorManager getNavigatorManager() {
        return this.navigatorManager;
    }

    @NotNull
    public static PlayerComponentCoordinator getManager(ServerPlayerEntity player) {
        return ((PeriodicTaskManagerInterface) player).carpet_Org_Addition$getPlayerPeriodicTaskManager();
    }

    @NotNull
    public static FakePlayerComponentCoordinator getManager(EntityPlayerMPFake fakePlayer) {
        return (FakePlayerComponentCoordinator) getManager((ServerPlayerEntity) fakePlayer);
    }

    /**
     * <p>
     * <s>玩家通过末地返回传送门时，实际上是创建了一个新对象，然后将原有的数据拷贝到了新对象上，而本类的对象也是玩家的一个成员变量，因此也要进行拷贝。</s>
     * </p>
     * <p>
     * 在{@code 1.21}中通过调试发现这个方法并没有在玩家进入返回传送门时执行，传送逻辑已经被修改了吗？
     * </p>
     */
    public void copyFrom(ServerPlayerEntity oldPlayer) {
        this.navigatorManager.setNavigatorFromOldPlayer(oldPlayer);
    }

    protected ServerPlayerEntity getPlayer() {
        return this.player;
    }

    public GameMode getNbtGameMode() {
        return this.nbtGameMode;
    }

    public void setNbtGameMode(GameMode gameMode) {
        this.nbtGameMode = gameMode;
    }
}
