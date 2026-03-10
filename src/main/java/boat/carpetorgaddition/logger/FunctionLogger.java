package boat.carpetorgaddition.logger;

import boat.carpetorgaddition.mixin.accessor.carpet.LoggerAccessor;
import boat.carpetorgaddition.util.PlayerUtils;
import carpet.logging.Logger;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Field;
import java.util.Map;

public class FunctionLogger extends Logger {
    private final Map<String, String> onlinePlayers = ((LoggerAccessor) this).getSubscribedOnlinePlayers();

    public FunctionLogger(Field acceleratorField, String logName, String def, String[] options, boolean strictOptions) {
        super(acceleratorField, logName, def, options, strictOptions);
    }

    public boolean isSubscribed(ServerPlayer player) {
        return this.onlinePlayers.containsKey(PlayerUtils.getName(player));
    }
}
