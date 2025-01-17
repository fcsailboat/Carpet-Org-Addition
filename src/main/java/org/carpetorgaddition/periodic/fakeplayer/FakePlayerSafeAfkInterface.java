package org.carpetorgaddition.periodic.fakeplayer;

public interface FakePlayerSafeAfkInterface {

    /**
     * 如果假玩家当前血量低于阈值，假玩家将立即退出游戏<br/>
     * 如果玩家血量小于0，则假玩家不会主动退出游戏
     */
    void setHealthThreshold(float threshold);

    float getHealthThreshold();

    /**
     * 安全挂机是否触发失败，这一般发生在最后一次受到的伤害超过了为假玩家设置的阈值
     */
    boolean afkTriggerFail();
}
