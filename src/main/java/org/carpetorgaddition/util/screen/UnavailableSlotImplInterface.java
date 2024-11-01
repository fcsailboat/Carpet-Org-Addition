package org.carpetorgaddition.util.screen;

import org.carpetorgaddition.network.s2c.UnavailableSlotSyncS2CPacket;

public interface UnavailableSlotImplInterface {
    /**
     * 通知客户端哪些槽位被禁用
     */
    void sync(UnavailableSlotSyncS2CPacket pack);
}
