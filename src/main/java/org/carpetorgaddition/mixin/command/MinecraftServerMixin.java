package org.carpetorgaddition.mixin.command;

import net.minecraft.server.MinecraftServer;
import org.carpetorgaddition.periodic.express.ExpressManager;
import org.carpetorgaddition.periodic.express.ExpressManagerInterface;
import org.carpetorgaddition.periodic.task.ServerTask;
import org.carpetorgaddition.periodic.task.ServerTaskManagerInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.function.BooleanSupplier;

@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements ServerTaskManagerInterface, ExpressManagerInterface {
    @Shadow
    public abstract void tick(BooleanSupplier shouldKeepTicking);

    @Unique
    private final MinecraftServer thisServer = (MinecraftServer) (Object) this;

    /**
     * 任务列表
     */
    @Unique
    private final ArrayList<ServerTask> tasks = new ArrayList<>();
    /**
     * 快递管理器
     */
    @Unique
    private ExpressManager expressManager;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(CallbackInfo ci) {
        expressManager = new ExpressManager(thisServer);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        this.tasks.removeIf(ServerTask::taskTick);
        this.expressManager.tick();
    }

    /**
     * @return 任务管理器
     */
    @Override
    public ArrayList<ServerTask> getTaskList() {
        return this.tasks;
    }

    @Override
    public void addTask(ServerTask task) {
        this.tasks.add(task);
    }

    /**
     * @return 快递管理器
     */
    @Override
    public ExpressManager getExpressManager() {
        return this.expressManager;
    }
}
