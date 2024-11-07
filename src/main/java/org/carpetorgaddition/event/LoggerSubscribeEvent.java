package org.carpetorgaddition.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * 记录器订阅事件
 */
public class LoggerSubscribeEvent {
    /**
     * 玩家订阅记录器时调用
     */
    public static final Event<Subscribe> SUBSCRIBE = EventFactory.createArrayBacked(Subscribe.class, callbacks -> (player, logName, option) -> {
        for (Subscribe callback : callbacks) {
            callback.accept(player, logName, option);
        }
    });

    /**
     * 玩家取消订阅记录器时调用
     */
    public static final Event<Unsubscribe> UNSUBSCRIBE = EventFactory.createArrayBacked(Unsubscribe.class, callbacks -> (player, logName) -> {
        for (Unsubscribe callback : callbacks) {
            callback.accept(player, logName);
        }
    });

    public interface Subscribe {
        void accept(ServerPlayerEntity player, String logName, String option);
    }

    public interface Unsubscribe {
        void accept(ServerPlayerEntity player, String logName);
    }
}
