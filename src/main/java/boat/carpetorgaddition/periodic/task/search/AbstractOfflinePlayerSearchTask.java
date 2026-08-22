package boat.carpetorgaddition.periodic.task.search;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.exception.FileOperationException;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.util.IOUtils;
import boat.carpetorgaddition.util.MathUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.GameProfileCache;
import boat.carpetorgaddition.wheel.ProgressBar;
import boat.carpetorgaddition.wheel.WorldFormat;
import boat.carpetorgaddition.wheel.inventory.FabricPlayerAccessManager;
import boat.carpetorgaddition.wheel.inventory.FabricPlayerAccessor;
import boat.carpetorgaddition.wheel.inventory.OfflinePlayerInventory;
import boat.carpetorgaddition.wheel.page.PageManager;
import boat.carpetorgaddition.wheel.page.PagedCollection;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.storage.FileNameDateFormatter;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractOfflinePlayerSearchTask extends ServerSearchTask {
    /**
     * 因文件损坏等原因暂时无法读取数据的玩家的UUID
     */
    private static final Set<UUID> CORRUPTED_PLAYER_DATAS = ConcurrentHashMap.newKeySet();
    /**
     * 已经备份过的文件，不再重新备份
     */
    private static final Set<UUID> BACKED_UP_FILES = ConcurrentHashMap.newKeySet();
    /**
     * 备份失败的文件，跳过查询
     */
    public static final Set<UUID> INVALID_PLAYER_DATAS = ConcurrentHashMap.newKeySet();
    public static final ScopedValue<UUID> CURRENT_UUID = ScopedValue.newInstance();
    public static final ScopedValue<Boolean> UPGRADING = ScopedValue.newInstance();
    public static final String UNKNOWN = "[Unknown]";
    private static final DateTimeFormatter FORMATTER = FileNameDateFormatter.FORMATTER;
    private static final ThreadPoolExecutor CPU_TASK_EXECUTOR = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors() + 1,
            Runtime.getRuntime().availableProcessors() + 1,
            5,
            TimeUnit.MINUTES,
            new LinkedBlockingQueue<>(),
            AbstractOfflinePlayerSearchTask::ofPlatformThread
    );
    /**
     * 查找线程的ID
     */
    private static final AtomicInteger CURRENT_THREAD_ID = new AtomicInteger(0);
    /**
     * 当前任务的数量
     */
    private final AtomicInteger taskCount = new AtomicInteger();
    /**
     * 备份文件夹所在位置
     */
    private final WorldFormat worldFormat;
    private final FabricPlayerAccessManager accessManager;
    /**
     * 总的玩家数量
     */
    protected int total = 0;
    protected final MinecraftServer server;
    private final File[] files;
    protected State taksState = State.START;
    /**
     * 备份文件夹的目录
     */
    private volatile WorldFormat backupFileDirectory;
    private final Object backupInitLock = new Object();
    protected final PagedCollection pagedCollection;
    /**
     * 开始查找的时间
     */
    private long startTime;
    /**
     * 查找进度
     */
    @Nullable
    private ProgressBar progressBar;
    /**
     * 已经完成查找的人数
     */
    private final AtomicInteger completedCount = new AtomicInteger(0);
    private volatile boolean cancelled = false;
    private static final long PROGRESS_BAR_WAIT_TIME = 1000L;

    static {
        CPU_TASK_EXECUTOR.allowCoreThreadTimeOut(true);
    }

    public AbstractOfflinePlayerSearchTask(CommandSourceStack source, ServerPlayer player) {
        super(source, player);
        this.server = ServerUtils.getServer(this.player);
        this.files = this.server.getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile().listFiles();
        if (this.files == null) {
            throw new IllegalStateException("Unable to read \"playerdata\" folder");
        }
        this.worldFormat = createWorldFormat(this.server);
        PageManager manager = ServerComponentCoordinator.of(this.server).getPageManager();
        this.pagedCollection = manager.newPagedCollection(this.source);
        this.accessManager = ServerComponentCoordinator.of(this.server).getAccessManager();
    }

    private static WorldFormat createWorldFormat(MinecraftServer server) {
        return new WorldFormat(server, "backups", "playerdata");
    }

    @Override
    protected void tick() {
        if (this.isTimeout()) {
            this.noticeCancelled();
        }
        this.checkCancelled();
        switch (this.taksState) {
            case START -> {
                this.start();
                this.taksState = State.RUNTIME;
                this.startTime = System.currentTimeMillis();
            }
            case RUNTIME -> {
                LocalizationKey key = this.getLocalizationKey().then("progress");
                if (this.taskCount.get() == 0) {
                    this.taksState = State.FEEDBACK;
                    if (this.progressBar != null) {
                        this.progressBar.setCompleted();
                    }
                } else if (MathUtils.timeDifference(this.startTime) >= PROGRESS_BAR_WAIT_TIME) {
                    if (this.progressBar == null) {
                        this.progressBar = new ProgressBar(this.total);
                    }
                    this.progressBar.setProgress(this.completedCount.get());
                }
                if (this.progressBar != null) {
                    this.sendProgress(key, this.progressBar);
                }
            }
            case FEEDBACK -> {
                boolean complete = this.sendFeedback();
                if (complete) {
                    this.taksState = State.STOP;
                }
            }
            case STOP -> {
            }
        }
    }

    protected abstract void sendProgress(LocalizationKey key, @NonNull ProgressBar progressBar);

    protected abstract boolean sendFeedback();

    /**
     * 开始搜索物品
     */
    private void start() {
        for (File file : this.files) {
            this.checkCancelled();
            if (file.getName().endsWith(".dat")) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(file.getName().split("\\.")[0]);
                } catch (IllegalArgumentException e) {
                    // 游戏在保存玩家数据时可能产生<玩家UUID>-<随机字符串>.dat文件
                    continue;
                }
                this.submit(file, uuid);
                this.total++;
            }
        }
    }

    /**
     * 提交任务
     */
    private void submit(File unsafe, UUID uuid) {
        this.taskCount.getAndIncrement();
        CPU_TASK_EXECUTOR.submit(() -> {
            try {
                if (this.isCancelled()) {
                    return;
                }
                if (INVALID_PLAYER_DATAS.contains(uuid)) {
                    return;
                }
                if (!this.isCancelled()) {
                    CompoundTag nbt = readNbt(unsafe, uuid);
                    if (nbt == null) {
                        return;
                    }
                    this.search(uuid, nbt);
                }
            } catch (CancellationException _) {
                // 更新玩家数据被中断，可能是玩家执行了/finder stop命令或服务器关闭，忽略异常
            } catch (RuntimeException | IOException e) {
                CarpetOrgAddition.LOGGER.error("Unable to read player data from file for file {}", unsafe.getName(), e);
                addCorruptedPlayerUUID(uuid);
            } finally {
                this.taskCount.getAndDecrement();
                this.completedCount.getAndIncrement();
            }
        });
    }

    protected void search(UUID uuid, CompoundTag nbt) {
        if (this.isCancelled()) {
            return;
        }
        // 获取玩家配置文件
        GameProfileCache cache = GameProfileCache.getInstance();
        Optional<NameAndId> optional = cache.getNameAndId(uuid);
        boolean unknownPlayer;
        if (optional.isEmpty()) {
            optional = Optional.of(new NameAndId(uuid, UNKNOWN));
            unknownPlayer = true;
        } else {
            unknownPlayer = false;
        }
        NameAndId entry = optional.get();
        // 不从在线玩家物品栏查找物品
        if (this.server.getPlayerList().getPlayerByName(entry.name()) != null) {
            return;
        }
        ScopedValue.where(CURRENT_UUID, uuid).run(() -> this.search(nbt, entry, unknownPlayer));
    }

    protected abstract void search(CompoundTag nbt, NameAndId entry, boolean unknownPlayer);

    /**
     * 读取玩家NBT数据，如果NBT版本低于当前游戏NBT版本，则先将数据备份再升级
     *
     * @param unsafe 玩家数据文件
     * @param uuid   玩家的UUID
     * @return 玩家的NBT数据
     */
    @Nullable
    private CompoundTag readNbt(File unsafe, UUID uuid) throws IOException {
        CompoundTag nbt = readNbt(unsafe);
        int version = NbtUtils.getDataVersion(nbt, -1);
        // 使用<而不是==，因为存档可能降级
        if (this.isCorruptedPlayerData(uuid) || version < ServerUtils.getMinecraftDataVersion()) {
            // 升级或修复玩家数据
            if (!this.isCancelled() && this.backupAndUpdate(unsafe, uuid)) {
                return readNbt(unsafe);
            }
            return null;
        } else {
            return nbt;
        }
    }

    private CompoundTag readNbt(File file) throws IOException {
        return NbtIo.readCompressed(file.toPath(), NbtAccounter.unlimitedHeap());
    }

    /**
     * 备份并更新玩家数据文件<br>
     * 如果NBT数据版本低，直接升级NBT并备份<br>
     * 如果读取物品数据时出错，则下一次查找物品时修复并备份
     *
     * @return 是否备份成功
     */
    private boolean backupAndUpdate(File unsafe, UUID uuid) {
        // 模拟玩家登录，更新玩家数据文件
        Optional<NameAndId> optional = GameProfileCache.getInstance()
                .getNameAndIdOrUnknown(uuid)
                .filter(entry -> ServerUtils.isPlayerDataExists(server, entry.id()));
        if (optional.isEmpty()) {
            return false;
        }
        NameAndId entry = optional.get();
        try {
            // 备份文件
            this.backup(unsafe, uuid);
        } catch (RuntimeException e) {
            // 备份失败的文件
            CarpetOrgAddition.LOGGER.warn("Player data has expired: {}", uuid, e);
            INVALID_PLAYER_DATAS.add(uuid);
            return false;
        }
        FabricPlayerAccessor accessor = ScopedValue.where(UPGRADING, true).call(() -> this.accessManager.getOrCreateBlocking(entry, this::isCancelled));
        OfflinePlayerInventory inventory = new OfflinePlayerInventory(accessor);
        inventory.setShowLog(false);
        inventory.startOpen(this.player);
        inventory.stopOpen(this.player);
        return true;
    }

    private void backup(File file, UUID uuid) {
        // 这里只会数据更新时执行一次，多次执行则认为数据已经不可修复
        if (this.shouldBackup(file, uuid)) {
            File backup = this.getBackupFileDirectory().file(file.getName());
            File parent = backup.getParentFile();
            if (parent.isDirectory() || parent.mkdirs()) {
                IOUtils.copyFile(file, backup);
                BACKED_UP_FILES.add(uuid);
                removeCorruptedPlayerUUID(uuid);
            } else {
                throw new FileOperationException();
            }
            return;
        }
        throw new IllegalStateException();
    }

    /**
     * 如果这名玩家的数据曾经备份过，则无需备份<br>
     * 如果这么玩家的数据已经存在于备份文件夹了，则无需备份<br>
     * 其它情况都需要备份
     *
     * @return 当前文件是否需要备份
     */
    private boolean shouldBackup(File file, UUID uuid) {
        // 已经有备份了，无需备份
        if (BACKED_UP_FILES.contains(uuid)) {
            return false;
        }
        // 正常情况下，list元素数量不多于1个
        List<String> list = this.worldFormat.listFiles()
                .stream()
                .filter(File::isDirectory)
                .map(File::getName)
                .filter(this::parseDirectoryName)
                .sorted()
                .toList();
        // 当前数据版本还没有备份
        if (list.isEmpty()) {
            return true;
        }
        // 获取最新的备份文件夹
        File directory = this.worldFormat.directory(list.getLast());
        // 已经有备份了，无需备份
        if (IOUtils.containsIdenticalFile(directory, file)) {
            BACKED_UP_FILES.add(uuid);
            return false;
        }
        return true;
    }

    /**
     * 检查文件夹名称是否是备份文件夹的名称
     */
    private boolean parseDirectoryName(String name) {
        String end = "_" + ServerUtils.CURRENT_DATA_VERSION;
        if (name.endsWith(end)) {
            String dateTimeFormat = name.substring(0, name.length() - end.length());
            try {
                FORMATTER.parse(dateTimeFormat);
                return true;
            } catch (DateTimeParseException e) {
                return false;
            }
        }
        return false;
    }

    /**
     * 为线程池创建线程
     */
    private static Thread ofPlatformThread(Runnable runnable) {
        return Thread.ofPlatform()
                .daemon()
                .name("OfflinePlayerItemSearch-Thread-" + CURRENT_THREAD_ID.getAndIncrement())
                .uncaughtExceptionHandler((_, e) -> CarpetOrgAddition.LOGGER.warn("An unexpected error occurred: ", e))
                .unstarted(runnable);
    }

    /**
     * @return 玩家数据是否已经损坏
     */
    private boolean isCorruptedPlayerData(UUID uuid) {
        return CORRUPTED_PLAYER_DATAS.contains(uuid);
    }

    /**
     * 将指定UUID玩家的数据标记为已损坏
     */
    public static void addCorruptedPlayerUUID(UUID uuid) {
        // 无法读取的玩家数据，下次不再读取
        if (CORRUPTED_PLAYER_DATAS.add(uuid)) {
            CarpetOrgAddition.LOGGER.warn("Unable to read player data from file for UUID {}", uuid.toString());
        }
    }

    public static void removeCorruptedPlayerUUID(UUID uuid) {
        CORRUPTED_PLAYER_DATAS.remove(uuid);
    }

    /**
     * 清理过期的备份文件
     */
    public static void cleanExpiredBackups(MinecraftServer server) {
        WorldFormat worldFormat = createWorldFormat(server);
        worldFormat.stream().filter(File::isDirectory).forEach(file -> {
            String fileName = file.getName();
            try {
                String[] split = fileName.split("_");
                if (split.length != 3) {
                    CarpetOrgAddition.LOGGER.debug("Skipped file with unexpected name format: {}", fileName);
                    return;
                }
                String time = "%s_%s".formatted(split[0], split[1]);
                LocalDateTime fileTime = LocalDateTime.from(FORMATTER.parse(time));
                long daysBetween = ChronoUnit.DAYS.between(fileTime, LocalDateTime.now());
                // 删除超过30天的备份文件
                if (daysBetween > 30) {
                    CarpetOrgAddition.LOGGER.info("Deleting expired backup: {} (age: {} days)", fileName, daysBetween);
                    removeBackupDirectory(file);
                }
            } catch (RuntimeException e) {
                CarpetOrgAddition.LOGGER.warn("Error processing backup file: {}", fileName, e);
            }
        });
    }

    private static void removeBackupDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            String fileName = file.getName();
            String[] split = fileName.split("\\.");
            // 删除UUID.dat的文件，其他文件不动
            // 一般情况下，文件夹内的文件都是这种格式的
            // 但如果确实包含其他文件，则不删除文件夹（在下方删除文件夹时会抛出DirectoryNotEmptyException）
            if (split.length == 2 && ServerUtils.isUuidString(split[0]) && "dat".equals(split[1])) {
                IOUtils.removeFileIfExists(file);
            }
        }
        IOUtils.removeFileIfExists(directory);
    }

    public static void clear() {
        CORRUPTED_PLAYER_DATAS.clear();
        BACKED_UP_FILES.clear();
        INVALID_PLAYER_DATAS.clear();
    }

    /**
     * @return 获取备份目录
     */
    private WorldFormat getBackupFileDirectory() {
        if (this.backupFileDirectory == null) {
            synchronized (this.backupInitLock) {
                if (this.backupFileDirectory == null) {
                    String time = LocalDateTime.now().format(FORMATTER) + "_" + ServerUtils.getMinecraftDataVersion();
                    this.backupFileDirectory = this.worldFormat.resolve(time);
                }
            }
        }
        return this.backupFileDirectory;
    }

    protected abstract LocalizationKey getLocalizationKey();

    @Override
    protected boolean stopped() {
        return this.taksState == State.STOP;
    }

    @Override
    public void cancel() {
        this.cancelled = true;
    }

    @Override
    public boolean isCancelled() {
        return ServerUtils.isStoping(this.server) || this.cancelled;
    }

    public enum State {
        START,
        RUNTIME,
        FEEDBACK,
        STOP
    }
}
