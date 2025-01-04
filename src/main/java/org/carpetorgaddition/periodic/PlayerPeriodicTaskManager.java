package org.carpetorgaddition.periodic;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.server.network.ServerPlayerEntity;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerActionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerPeriodicTaskManager {
    @Nullable
    private final FakePlayerActionManager fakePlayerActionManager;

    public PlayerPeriodicTaskManager(ServerPlayerEntity player) {
        if (player instanceof EntityPlayerMPFake fakePlayer) {
            this.fakePlayerActionManager = new FakePlayerActionManager(fakePlayer);
        } else {
            this.fakePlayerActionManager = null;
        }
    }

    public void tick() {
        if (this.fakePlayerActionManager != null) {
            this.fakePlayerActionManager.tick();
        }
    }

    @Nullable
    public FakePlayerActionManager getFakePlayerActionManager() {
        return fakePlayerActionManager;
    }


    @NotNull
    public static PlayerPeriodicTaskManager getManager(ServerPlayerEntity player) {
        return ((PeriodicTaskManagerInterface) player).carpet_Org_Addition$getPlayerPeriodicTaskManager();
    }

    /**
     * 玩家通过末地返回传送门时，实际上是创建了一个新对象，然后将原有的数据拷贝到了新对象上，而本类的对象也是玩家的一个成员变量，因此也要进行拷贝。
     *
     * @return 当前对象的副本
     */
    public PlayerPeriodicTaskManager copyFrom(ServerPlayerEntity newPlayer, ServerPlayerEntity oldPlayer) {
        PlayerPeriodicTaskManager copy = new PlayerPeriodicTaskManager(newPlayer);
        if (copy.fakePlayerActionManager != null) {
            copy.fakePlayerActionManager.setActionFromOldPlayer((EntityPlayerMPFake) oldPlayer);
        }
        return copy;
    }
}
