package org.carpetorgaddition.wheel.inventory;

import com.google.common.collect.Queues;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class FabricPlayerAccessManager {
    private final MinecraftServer server;
    /**
     * 正在操作物品栏的玩家，键表示被打开物品栏玩家的配置文件，值表示正在打开物品栏的玩家集合
     */
    private final Map<GameProfile, Set<ServerPlayerEntity>> viewers = new ConcurrentHashMap<>();
    /**
     * 配置文件对应的Fabric玩家访问器
     */
    private final Map<GameProfile, FabricPlayerAccessor> accessors = new ConcurrentHashMap<>();
    private final Queue<FabricPlayerAccessorEntry> queue = Queues.newConcurrentLinkedQueue();
    /**
     * 上次处理队列的时间
     */
    private volatile long lastProcessingTime = System.currentTimeMillis();
    private static final long MAX_TICK_TACK_TIME = 25L;
    private static final long HEALTH_CHECK_TIMEOUT_MS = 8000L;
    private static final long AWAIT_TIMEOUT_MS = 1000L;

    public FabricPlayerAccessManager(MinecraftServer server) {
        this.server = server;
    }

    public void tick() {
        if (this.queue.isEmpty()) {
            return;
        }
        this.server.execute(this::createFabricPlayerAccessor);
    }

    public FabricPlayerAccessor getOrCreate(GameProfile gameProfile) {
        return this.accessors.computeIfAbsent(gameProfile, profile -> new FabricPlayerAccessor(this.server, profile, this));
    }

    @NotNull
    public FabricPlayerAccessor getOrCreateBlocking(GameProfile gameProfile) {
        return this.accessors.computeIfAbsent(gameProfile, __ -> {
            // 在多个线程调用构造方法存在并发问题
            Supplier<FabricPlayerAccessor> supplier = () -> new FabricPlayerAccessor(this.server, gameProfile, this);
            FabricPlayerAccessorEntry entry = new FabricPlayerAccessorEntry(supplier);
            this.queue.add(entry);
            Lock lock = entry.getLock();
            Condition condition = entry.getCondition();
            try {
                lock.lock();
                while (true) {
                    RuntimeException exception = entry.getException();
                    if (exception != null) {
                        throw new IllegalStateException(exception);
                    }
                    FabricPlayerAccessor first = entry.getAccessor();
                    if (first != null) {
                        return first;
                    }
                    //noinspection ResultOfMethodCallIgnored
                    condition.await(AWAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                    if (this.server.isRunning()) {
                        FabricPlayerAccessor second = entry.getAccessor();
                        if (second != null) {
                            return second;
                        }
                        // 长时间没有任务被处理，可能发生了阻塞
                        long time = System.currentTimeMillis() - this.lastProcessingTime;
                        if (time > HEALTH_CHECK_TIMEOUT_MS) {
                            throw new IllegalStateException("FabricPlayerAccessManager blocked " + time + "ms, queue size:" + this.queue.size() + ", profile:" + gameProfile);
                        }
                    } else {
                        throw new IllegalStateException("The server has been shut down");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            } finally {
                lock.unlock();
            }
        });
    }

    private void createFabricPlayerAccessor() {
        long time = System.currentTimeMillis();
        while (true) {
            // 将剩余任务推迟到下一个游戏刻执行
            if (System.currentTimeMillis() - time > MAX_TICK_TACK_TIME) {
                return;
            }
            FabricPlayerAccessorEntry entry = this.queue.poll();
            if (entry == null) {
                return;
            }
            Lock lock = entry.getLock();
            Condition condition = entry.getCondition();
            try {
                lock.lock();
                try {
                    entry.createAccessor();
                } catch (RuntimeException e) {
                    entry.setException(e);
                } finally {
                    condition.signalAll();
                }
            } finally {
                lock.unlock();
            }
            // 任务被执行了，更新上次执行任务的时间
            this.lastProcessingTime = System.currentTimeMillis();
        }
    }

    /**
     * @return 是否有玩家正在查看离线玩家物品栏
     */
    public boolean hasViewers() {
        return !this.viewers.isEmpty();
    }

    /**
     * 获取所有正在查看指定离线玩家物品栏的玩家
     */
    public Set<ServerPlayerEntity> getViewers(GameProfile gameProfile) {
        Set<ServerPlayerEntity> set = this.viewers.get(gameProfile);
        return set == null ? Set.of() : set;
    }

    public void addViewers(GameProfile gameProfile, Set<ServerPlayerEntity> viewers) {
        this.viewers.put(gameProfile, viewers);
    }

    public void removeAccessor(GameProfile gameProfile) {
        this.accessors.remove(gameProfile);
    }

    public void removeViewer(GameProfile gameProfile) {
        this.viewers.remove(gameProfile);
    }

    public static class FabricPlayerAccessorEntry {
        private final Supplier<FabricPlayerAccessor> supplier;
        private final AtomicReference<FabricPlayerAccessor> accessor;
        private final AtomicReference<RuntimeException> exception;
        private final Lock lock = new ReentrantLock();
        private final Condition condition = this.lock.newCondition();

        private FabricPlayerAccessorEntry(Supplier<FabricPlayerAccessor> supplier) {
            this.supplier = supplier;
            this.accessor = new AtomicReference<>();
            this.exception = new AtomicReference<>();
        }

        private void createAccessor() {
            this.accessor.set(this.supplier.get());
        }

        private void setException(RuntimeException exception) {
            this.exception.set(exception);
        }

        private FabricPlayerAccessor getAccessor() {
            return this.accessor.get();
        }

        private RuntimeException getException() {
            return this.exception.get();
        }

        private Lock getLock() {
            return this.lock;
        }

        private Condition getCondition() {
            return this.condition;
        }
    }
}
