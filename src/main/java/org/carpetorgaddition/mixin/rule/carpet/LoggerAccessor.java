package org.carpetorgaddition.mixin.rule.carpet;

import carpet.logging.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(value = Logger.class, remap = false)
public interface LoggerAccessor {
    @Accessor("subscribedOnlinePlayers")
    Map<String, String> getSubscribedOnlinePlayers();
}
