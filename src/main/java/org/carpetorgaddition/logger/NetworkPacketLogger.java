package org.carpetorgaddition.logger;

import carpet.logging.Logger;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import org.carpetorgaddition.mixin.rule.carpet.LoggerAccessor;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public class NetworkPacketLogger extends Logger {
    public final Map<String, String> onlinePlayers = ((LoggerAccessor) this).getSubscribedOnlinePlayers();

    public NetworkPacketLogger(Field acceleratorField, String logName, String def, String[] options, boolean strictOptions) {
        super(acceleratorField, logName, def, options, strictOptions);
    }

    public void sendPacket(Supplier<CustomPayload> supplier) {
        this.onlinePlayers
                .keySet()
                .stream()
                .map(this::playerFromName)
                .filter(Objects::nonNull)
                .forEach(player -> ServerPlayNetworking.send(player, supplier.get()));
    }

    public void sendPacket(Function<String, CustomPayload> function) {
        for (Map.Entry<String, String> entry : this.onlinePlayers.entrySet()) {
            if (this.playerFromName(entry.getKey()) instanceof ServerPlayerEntity player) {
                ServerPlayNetworking.send(player, function.apply(entry.getValue()));
            }
        }
    }

    public void sendPacketIfOnline(ServerPlayerEntity player, Supplier<CustomPayload> supplier) {
        if (this.onlinePlayers.containsKey(player.getName().getString())) {
            ServerPlayNetworking.send(player, supplier.get());
        }
    }
}
