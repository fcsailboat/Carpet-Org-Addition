package org.carpetorgaddition.periodic;

import net.minecraft.server.MinecraftServer;
import org.carpetorgaddition.periodic.express.ExpressManager;
import org.carpetorgaddition.periodic.task.ServerTask;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class ServerPeriodicTaskManager {
    /**
     * 快递管理器
     */
    private final ExpressManager expressManager;
    /**
     * 服务器任务列表
     */
    private final ArrayList<ServerTask> serverTaskList = new ArrayList<>();

    public ServerPeriodicTaskManager(@NotNull MinecraftServer server) {
        this.expressManager = new ExpressManager(server);
    }

    public void tick() {
        this.expressManager.tick();
        this.serverTaskList.removeIf(ServerTask::taskTick);
    }
}
