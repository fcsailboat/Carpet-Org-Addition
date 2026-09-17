package boat.carpetorgaddition.wheel.mixin;

import boat.carpetorgaddition.network.s2c.UnavailableSlotSyncS2CPacket;

public interface UnavailableSlotClientSide {
    /**
     * 通知客户端哪些槽位被禁用
     */
    void carpet_Org_Addition$sync(UnavailableSlotSyncS2CPacket pack);
}
