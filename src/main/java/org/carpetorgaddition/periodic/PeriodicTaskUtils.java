package org.carpetorgaddition.periodic;

import carpet.patches.EntityPlayerMPFake;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerActionManager;
import org.jetbrains.annotations.Contract;

public class PeriodicTaskUtils {
    /**
     * 获取一名假玩家的动作管理器，永远不会返回null
     *
     * @apiNote 此方法的作用是避免IDE发出 {@code NullPointerException} 警告
     */
    @Contract("_ -> !null")
    public static FakePlayerActionManager getFakePlayerActionManager(EntityPlayerMPFake fakePlayer) {
        return PlayerPeriodicTaskManager.getManager(fakePlayer).getFakePlayerActionManager();
    }
}
