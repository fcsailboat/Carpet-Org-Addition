package boat.carpetorgaddition.periodic.task;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.debug.DebugSettings;
import boat.carpetorgaddition.exception.TaskExecutionException;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.ServerTickRateManager;

public abstract class ServerTask {
    protected final CommandSourceStack source;
    private long tickTaskStartTime = -1L;
    /**
     * 任务开始执行的时间
     */
    private long startTime = -1L;
    /**
     * 该任务是否应该被删除
     */
    private boolean remove = false;

    public ServerTask(CommandSourceStack source) {
        this.source = source;
    }

    /**
     * 每个游戏刻都调用此方法
     */
    protected abstract void tick();

    /**
     * @return 当前任务是否已经执行完毕
     */
    protected abstract boolean stopped();

    /**
     * 执行任务
     *
     * @return 当前任务是否已经执行结束
     */
    public final boolean execute(ServerTickRateManager tickManager) {
        if (this.remove) {
            return true;
        }
        try {
            if (tickManager.runsNormally() || this.constantSpeed()) {
                this.tickTaskStartTime = System.currentTimeMillis();
                this.tick();
                this.tickTaskStartTime = -1L;
                return this.stopped();
            } else {
                return false;
            }
        } catch (TaskExecutionException e) {
            e.disposal();
        } catch (RuntimeException e) {
            CarpetOrgAddition.LOGGER.error("{} encountered an unexpected error while executing the task", this.getClass().getSimpleName(), e);
        }
        return true;
    }

    /**
     * 检查当前任务是否超时，如果超时，抛出异常
     */
    protected void checkTimeout() {
        long time = this.getMaxExecutionTime();
        if (time == -1L) {
            return;
        }
        if (this.getElapsedTime() > time) {
            // 任务超时
            throw new TaskExecutionException(this::timeoutHandler);
        }
    }

    /**
     * @return 任务开始执行到现在所经过的时间
     */
    protected long getElapsedTime() {
        if (this.startTime == -1L) {
            return 0;
        }
        return System.currentTimeMillis() - this.startTime;
    }

    /**
     * 获取当前任务本tick执行的时间
     */
    private long getTickExecutionTime() {
        if (this.tickTaskStartTime == -1L) {
            return 0L;
        }
        return System.currentTimeMillis() - this.tickTaskStartTime;
    }

    /**
     * @return 任务的最大执行时间
     */
    protected long getMaxExecutionTime() {
        return -1L;
    }

    /**
     * @return 任务每个tick最大执行的时间
     */
    protected long getMaxTimeSlice() {
        return -1L;
    }

    /**
     * 当前tick还有可执行时间
     */
    protected boolean isTimeRemaining() {
        long slice = this.getMaxTimeSlice();
        if (slice == -1L) {
            return true;
        }
        return (CarpetOrgAddition.isDevelopment() && DebugSettings.prohibitTaskTimeout.get()) || this.getTickExecutionTime() < slice;
    }

    /**
     * 当前tick没有可执行时间了
     */
    protected boolean isTimeExpired() {
        return !this.isTimeRemaining();
    }

    /**
     * 超时的处理策略
     */
    protected void timeoutHandler() {
        MessageUtils.sendErrorMessage(this.source, LocalizationKeys.Operation.Timeout.TASK.translate());
    }

    /**
     * @return 此任务是否不受/tick命令影响
     */
    public boolean constantSpeed() {
        return true;
    }

    /**
     * 将当前任务标记为已删除
     */
    public void markRemove() {
        this.remove = true;
    }

    /**
     * 设置任务开始时间
     */
    public final void setStartTime() {
        this.startTime = System.currentTimeMillis();
    }

    /**
     * 当任务开始时执行
     */
    public void onStarted() {
    }

    /**
     * 当任务结束时执行
     */
    public void onStopped() {
    }
}
