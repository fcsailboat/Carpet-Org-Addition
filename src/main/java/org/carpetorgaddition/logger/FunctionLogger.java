package org.carpetorgaddition.logger;

import carpet.logging.Logger;
import net.minecraft.server.network.ServerPlayerEntity;
import org.carpetorgaddition.mixin.rule.carpet.LoggerAccessor;

import java.lang.reflect.Field;
import java.util.Map;

public class FunctionLogger extends Logger {
    private final Map<String, String> onlinePlayers = ((LoggerAccessor) this).getSubscribedOnlinePlayers();

    public FunctionLogger(Field acceleratorField, String logName, String def, String[] options, boolean strictOptions) {
        super(acceleratorField, logName, def, options, strictOptions);
    }

    public boolean isSubscribed(ServerPlayerEntity player) {
        return this.onlinePlayers.containsKey(player.getName().getString());
    }
}
