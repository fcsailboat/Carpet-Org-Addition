package org.carpetorgaddition.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * 玩家打开物品栏时调用
 */
public class PlayerOpenHandledScreenEvent {
    /**
     * 玩家打开物品栏后的事件，在此阶段服务器已经向客户端发送{@code OpenScreenS2CPacket}数据包
     */
    public static final Event<AlreadyOpened> ALREADY_OPENED = EventFactory.createArrayBacked(AlreadyOpened.class, callbacks -> (player, screenHandler) -> {
        for (AlreadyOpened callback : callbacks) {
            callback.after(player, screenHandler);
        }
    });


    @FunctionalInterface
    public interface AlreadyOpened {
        void after(ServerPlayerEntity player, ScreenHandler screenHandler);
    }
}