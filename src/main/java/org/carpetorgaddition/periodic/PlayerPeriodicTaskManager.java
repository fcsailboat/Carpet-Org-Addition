package org.carpetorgaddition.periodic;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.server.ServerTickManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerActionManager;
import org.carpetorgaddition.periodic.navigator.NavigatorManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerPeriodicTaskManager {
    @Nullable
    private final FakePlayerActionManager fakePlayerActionManager;
    private final NavigatorManager navigatorManager;
    private final ServerPlayerEntity player;

    public PlayerPeriodicTaskManager(ServerPlayerEntity player) {
        this.player = player;
        if (player instanceof EntityPlayerMPFake fakePlayer) {
            this.fakePlayerActionManager = new FakePlayerActionManager(fakePlayer);
        } else {
            this.fakePlayerActionManager = null;
        }
        this.navigatorManager = new NavigatorManager(player);
    }

    public void tick() {
        if (this.fakePlayerActionManager != null) {
            ServerTickManager tickManager = this.player.server.getTickManager();
            if (tickManager.shouldTick()) {
                this.fakePlayerActionManager.tick();
            }
        }
        this.navigatorManager.tick();
    }

    @Nullable
    public FakePlayerActionManager getFakePlayerActionManager() {
        return this.fakePlayerActionManager;
    }

    public NavigatorManager getNavigatorManager() {
        return this.navigatorManager;
    }


    @NotNull
    public static PlayerPeriodicTaskManager getManager(ServerPlayerEntity player) {
        return ((PeriodicTaskManagerInterface) player).carpet_Org_Addition$getPlayerPeriodicTaskManager();
    }

    /**
     * 玩家通过末地返回传送门时，实际上是创建了一个新对象，然后将原有的数据拷贝到了新对象上，而本类的对象也是玩家的一个成员变量，因此也要进行拷贝。
     */
    public void copyFrom(ServerPlayerEntity oldPlayer) {
        if (this.fakePlayerActionManager != null) {
            this.fakePlayerActionManager.setActionFromOldPlayer((EntityPlayerMPFake) oldPlayer);
        }
        this.navigatorManager.setNavigatorFromOldPlayer(oldPlayer);
    }
}
