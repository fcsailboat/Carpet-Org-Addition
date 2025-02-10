package org.carpetorgaddition.util;

import carpet.patches.EntityPlayerMPFake;
import org.carpetorgaddition.periodic.PlayerPeriodicTaskManager;
import org.carpetorgaddition.periodic.fakeplayer.BlockBreakManager;
import org.carpetorgaddition.periodic.fakeplayer.action.FakePlayerActionManager;
import org.jetbrains.annotations.Contract;

public class GenericFetcherUtils {
    /**
     * 获取一名假玩家的动作管理器，永远不会返回null
     *
     * @apiNote 此方法的作用是避免IDE发出 {@code NullPointerException} 警告
     */
    @Contract("_ -> !null")
    public static FakePlayerActionManager getFakePlayerActionManager(EntityPlayerMPFake fakePlayer) {
        return PlayerPeriodicTaskManager.getManager(fakePlayer).getFakePlayerActionManager();
    }

    @Contract("_ -> !null")
    public static BlockBreakManager getBlockBreakManager(EntityPlayerMPFake fakePlayer) {
        return PlayerPeriodicTaskManager.getManager(fakePlayer).getBlockBreakManager();
    }
}
