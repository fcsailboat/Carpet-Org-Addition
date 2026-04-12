package boat.carpetorgaddition.periodic.task;

import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.ServerTickRateManager;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * 服务器任务管理器
 */
public class ServerTaskManager {
    private final Set<ServerTask> tasks = ConcurrentHashMap.newKeySet();

    public ServerTaskManager() {
    }

    /**
     * 添加一条新任务
     *
     * @throws CommandSyntaxException 如果任务已经存在，抛出此异常
     */
    public void addTask(ServerTask task) throws CommandSyntaxException {
        if (this.tasks.add(task)) {
            task.setStartTime();
            task.onStarted();
            return;
        }
        throw CommandUtils.createException(LocalizationKeys.Operation.WAIT_LAST.translate());
    }

    /**
     * 执行每一条任务，并删除已经结束的任务
     */
    public void tick(ServerTickRateManager tickManager) {
        this.tasks.removeIf(task -> {
            boolean completed = task.execute(tickManager);
            if (completed) {
                task.onStopped();
            }
            return completed;
        });
    }

    public Stream<ServerTask> stream() {
        return this.tasks.stream();
    }

    public <T> Stream<T> stream(Class<T> classFilter) {
        return this.tasks.stream().filter(classFilter::isInstance).map(classFilter::cast);
    }
}
