package boat.carpetorgaddition.logger;

import boat.carpetorgaddition.util.PlayerUtils;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import carpet.logging.Logger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.function.Consumer;

public final class LoggerAccessor {
    private final Logger logger;
    private final String name;
    private final boolean hidden;
    private final Consumer<ServerPlayer> subscribeCallback;
    private final Consumer<ServerPlayer> unsubscribeCallback;

    public LoggerAccessor(Logger logger, String name, boolean hidden, Consumer<ServerPlayer> subscribeCallback, Consumer<ServerPlayer> unsubscribeCallback) {
        this.logger = logger;
        this.name = name;
        this.hidden = hidden;
        this.subscribeCallback = subscribeCallback;
        this.unsubscribeCallback = unsubscribeCallback;
    }

    public boolean isEnable() {
        return this.getLogger().hasOnlineSubscribers();
    }

    public boolean isSubscribed(ServerPlayer player) {
        return getSubscribedOnlinePlayers().containsKey(PlayerUtils.getName(player));
    }

    public Map<String, String> getSubscribedOnlinePlayers() {
        return ((boat.carpetorgaddition.mixin.accessor.carpet.LoggerAccessor) this.logger).getSubscribedOnlinePlayers();
    }

    public Logger getLogger() {
        return this.logger;
    }

    public String getName() {
        return this.name;
    }

    public LocalizationKey getLocalizationKey() {
        return LocalizationKeys.LOGGER.then(this.name);
    }

    public boolean isHidden() {
        return this.hidden;
    }

    @SuppressWarnings("unused")
    public void onSubscribe(ServerPlayer player) {
        this.subscribeCallback.accept(player);
    }

    public void onUnsubscribe(ServerPlayer player) {
        this.unsubscribeCallback.accept(player);
    }
}
