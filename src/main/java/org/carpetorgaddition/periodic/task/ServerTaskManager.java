package org.carpetorgaddition.periodic.task;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.carpetorgaddition.util.CommandUtils;

import java.util.HashSet;
import java.util.stream.Stream;

/**
 * 服务器任务管理器
 */
public class ServerTaskManager {
    private final HashSet<ServerTask> tasks = new HashSet<>();

    public ServerTaskManager() {
    }

    /**
     * 添加一条新任务
     *
     * @throws CommandSyntaxException 如果任务已经存在，抛出此异常
     */
    public void addTask(ServerTask task) throws CommandSyntaxException {
        if (this.tasks.add(task)) {
            return;
        }
        throw CommandUtils.createException("carpet.commands.finder.add.exist");
    }

    /**
     * 执行每一条任务，并删除已经结束的任务
     */
    public void tick() {
        this.tasks.removeIf(ServerTask::execute);
    }

    public Stream<ServerTask> stream() {
        return this.tasks.stream();
    }

    public <T> Stream<T> stream(Class<T> clazz) {
        return this.tasks.stream().filter(clazz::isInstance).map(clazz::cast);
    }
}
