package boat.carpetorgaddition.periodic.task;

import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * 服务器任务管理器
 */
public class ServerTaskManager {
    private final Map<Object, ServerTask> tasks = new ConcurrentHashMap<>();

    public ServerTaskManager() {
    }

    /**
     * 添加一条新任务
     *
     * @throws CommandSyntaxException 如果任务已经存在，抛出此异常
     */
    public void addTask(ServerTask task) throws CommandSyntaxException {
        Object identityKey = task.getIdentityKey();
        if (this.tasks.containsKey(identityKey)) {
            createWaitLastException();
        }
        this.tasks.put(identityKey, task);
        task.setStartTime();
        task.onStarted();
    }

    public void addTask(ServerTask task, ServerPlayer player) throws CommandSyntaxException {
        Object identityKey = task.getIdentityKey();
        if (this.tasks.containsKey(identityKey)) {
            ServerTask another = this.tasks.get(identityKey);
            if (Objects.equals(another.getSource().getPlayer(), player)) {
                createWaitLastException();
            } else {
                throw CommandUtils.createException(LocalizationKeys.Operation.WAIT_PLAYER.translate(another.getSource().getDisplayName()));
            }
        }
        this.tasks.put(identityKey, task);
        task.setStartTime();
        task.onStarted();
    }

    private static void createWaitLastException() throws CommandSyntaxException {
        throw CommandUtils.createException(LocalizationKeys.Operation.WAIT_LAST.translate());
    }

    /**
     * 执行每一条任务，并删除已经结束的任务
     */
    public void tick(ServerTickRateManager tickManager) {
        this.tasks.entrySet().removeIf(entry -> {
            ServerTask task = entry.getValue();
            boolean completed = task.execute(tickManager);
            if (completed) {
                task.onStopped();
            }
            return completed;
        });
    }

    public Stream<ServerTask> stream() {
        return this.tasks.values().stream();
    }

    public <T> Stream<T> stream(Class<T> classFilter) {
        return this.stream().filter(classFilter::isInstance).map(classFilter::cast);
    }
}
