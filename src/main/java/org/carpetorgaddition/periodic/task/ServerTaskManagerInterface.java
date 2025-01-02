package org.carpetorgaddition.periodic.task;

import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public interface ServerTaskManagerInterface {
    void addTask(ServerTask task);

    /**
     * @return 获取任务列表
     */
    ArrayList<ServerTask> getTaskList();

    /**
     * 查找符合条件的任务
     *
     * @param clazz     用来判断任务对象是否是T类的对象
     * @param predicate 条件谓词
     * @return 包含所有符合条件的任务的集合
     */
    default <T> List<T> findTask(Class<T> clazz, Predicate<T> predicate) {
        return this.getTaskList().stream()
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .filter(predicate)
                .collect(Collectors.toList());
    }

    @NotNull
    static ServerTaskManagerInterface getInstance(MinecraftServer server) {
        return (ServerTaskManagerInterface) server;
    }
}
