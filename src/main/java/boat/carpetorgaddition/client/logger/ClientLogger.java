package boat.carpetorgaddition.client.logger;


import boat.carpetorgaddition.network.s2c.LoggerUpdateS2CPacket;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Optional;

@SuppressWarnings("unused")
public class ClientLogger {
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private static final HashMap<String, Optional<String>> subscriptions = new HashMap<>();

    private static void put(String logger, @Nullable String option) {
        subscriptions.put(logger, Optional.ofNullable(option));
    }

    private static void remove(String logger) {
        subscriptions.remove(logger);
        onRemove(logger);
    }

    public static void onPacketReceive(LoggerUpdateS2CPacket packet) {
        if (packet.remove()) {
            remove(packet.name());
        } else {
            put(packet.name(), packet.option());
        }
    }

    private static void onRemove(String logger) {
    }
}
