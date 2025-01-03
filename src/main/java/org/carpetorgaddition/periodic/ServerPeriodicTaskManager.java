package org.carpetorgaddition.periodic;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import org.carpetorgaddition.periodic.express.ExpressManager;
import org.carpetorgaddition.periodic.task.ServerTaskManager;
import org.jetbrains.annotations.NotNull;

/**
 * 用来统一管理服务器周期任务
 */
public class ServerPeriodicTaskManager {
    /**
     * 快递管理器
     */
    private final ExpressManager expressManager;
    /**
     * 服务器任务管理器
     */
    private final ServerTaskManager serverTaskManager = new ServerTaskManager();

    public ServerPeriodicTaskManager(@NotNull MinecraftServer server) {
        this.expressManager = new ExpressManager(server);
    }

    public void tick() {
        //  this.expressManager.tick();
        this.serverTaskManager.tick();
    }

    public ServerTaskManager getServerTaskManager() {
        return serverTaskManager;
    }

    @NotNull
    public static ServerPeriodicTaskManager getManager(CommandContext<ServerCommandSource> context) {
        return getManager(context.getSource().getServer());
    }

    @NotNull
    public static ServerPeriodicTaskManager getManager(MinecraftServer server) {
        return ((PeriodicTaskManagerInterface) server).carpet_Org_Addition$getServerPeriodicTaskManager();
    }
}
