package org.carpetorgaddition.periodic.fakeplayer;

import carpet.patches.EntityPlayerMPFake;

// 假玩家动作接口
public interface FakePlayerActionInterface {
    FakePlayerActionManager getActionManager();

    // 从旧玩家拷贝动作管理器
    void copyActionManager(EntityPlayerMPFake fakePlayer);

    // 获取假玩家动作管理器对象
    static FakePlayerActionManager getManager(EntityPlayerMPFake fakePlayer) {
        return ((FakePlayerActionInterface) fakePlayer).getActionManager();
    }
}
