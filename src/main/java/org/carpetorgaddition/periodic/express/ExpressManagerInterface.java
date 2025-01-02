package org.carpetorgaddition.periodic.express;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;

public interface ExpressManagerInterface {
    ExpressManager getExpressManager();

    static ExpressManager getInstance(CommandContext<ServerCommandSource> context) {
        return getInstance(context.getSource().getServer());
    }

    static ExpressManager getInstance(ServerCommandSource source) {
        return getInstance(source.getServer());
    }

    static ExpressManager getInstance(MinecraftServer server) {
        return ((ExpressManagerInterface) server).getExpressManager();
    }
}
