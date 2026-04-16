package org.carpetorgaddition.periodic.task;

import net.minecraft.server.ServerTickManager;
import net.minecraft.server.command.ServerCommandSource;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.exception.TaskExecutionException;
import org.carpetorgaddition.util.MessageUtils;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class ServerTask implements Thread.UncaughtExceptionHandler {
    protected final ServerCommandSource source;
    /**
     * 之前每个游戏刻中任务持续时间的总和
     */
    private long executionTime;
    private long tickTaskStartTime = -1L;
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors() + 1,
            Runtime.getRuntime().availableProcessors() + 1,
            5,
            TimeUnit.MINUTES,
            new LinkedBlockingQueue<>(),
            this::ofPlatformThread
    );

    public ServerTask(ServerCommandSource source) {
        this.source = source;
        this.executor.allowCoreThreadTimeOut(true);
    }

    /**
     * 该任务是否应该被删除
     */
    private boolean remove = false;

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
    public final boolean execute(ServerTickManager tickManager) {
        if (this.remove) {
            return true;
        }
        try {
            if (tickManager.shouldTick() || this.constantSpeed()) {
                this.tickTaskStartTime = System.currentTimeMillis();
                this.tick();
                this.executionTime += getTickExecutionTime();
                this.tickTaskStartTime = -1L;
                return this.stopped();
            } else {
                return false;
            }
        } catch (TaskExecutionException e) {
            e.disposal();
        } catch (RuntimeException e) {
            CarpetOrgAddition.LOGGER.error("{} encountered an unexpected error while executing the task", this.getLogName(), e);
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
        if (this.getExecutionTime() > time) {
            // 任务超时
            timeout();
        }
    }

    protected void timeout() {
        throw new TaskExecutionException(this::timeoutHandler);
    }

    /**
     * 获取当前任务已经执行的时间
     */
    protected long getExecutionTime() {
        if (this.tickTaskStartTime == -1L) {
            return this.executionTime;
        }
        return this.executionTime + getTickExecutionTime();
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
        return this.getTickExecutionTime() < slice;
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
        MessageUtils.sendErrorMessage(this.source, "carpet.command.task.timeout");
    }

    /**
     * @return 当前任务的名称，不在游戏中使用，只在日志中使用
     */
    public String getLogName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();

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

    protected void submit(Runnable task) {
        this.executor.submit(task);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        CarpetOrgAddition.LOGGER.warn("An unexpected error occurred: ", e);
    }

    /**
     * 为线程池创建线程
     */
    private Thread ofPlatformThread(Runnable runnable) {
        return Thread.ofPlatform()
                .daemon()
                .name(this.getClass().getSimpleName() + "-Thread")
                .uncaughtExceptionHandler(this)
                .unstarted(runnable);
    }
}
