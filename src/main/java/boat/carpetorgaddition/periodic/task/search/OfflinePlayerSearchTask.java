package boat.carpetorgaddition.periodic.task.search;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.command.FinderCommand;
import boat.carpetorgaddition.exception.FileOperationException;
import boat.carpetorgaddition.network.event.CustomClickAction;
import boat.carpetorgaddition.network.event.CustomClickEvents;
import boat.carpetorgaddition.network.event.CustomClickKeys;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.periodic.task.ServerTask;
import boat.carpetorgaddition.rule.value.OpenPlayerInventoryCommandOption;
import boat.carpetorgaddition.util.*;
import boat.carpetorgaddition.wheel.GameProfileCache;
import boat.carpetorgaddition.wheel.ItemStackStatistics;
import boat.carpetorgaddition.wheel.ProgressBar;
import boat.carpetorgaddition.wheel.WorldFormat;
import boat.carpetorgaddition.wheel.inventory.*;
import boat.carpetorgaddition.wheel.nbt.NbtWriter;
import boat.carpetorgaddition.wheel.page.PageManager;
import boat.carpetorgaddition.wheel.page.PagedCollection;
import boat.carpetorgaddition.wheel.predicate.ItemStackPredicate;
import boat.carpetorgaddition.wheel.provider.CommandProvider;
import boat.carpetorgaddition.wheel.provider.TextProvider;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import carpet.CarpetSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.FileNameDateFormatter;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class OfflinePlayerSearchTask extends ServerTask {
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
    public static final ThreadLocal<UUID> CURRENT_UUID = new ThreadLocal<>();
    public static final String UNKNOWN = "[Unknown]";
    private static final DateTimeFormatter FORMATTER = FileNameDateFormatter.FORMATTER;
    private static final ThreadPoolExecutor CPU_TASK_EXECUTOR = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors() + 1,
            Runtime.getRuntime().availableProcessors() + 1,
            5,
            TimeUnit.MINUTES,
            new LinkedBlockingQueue<>(),
            OfflinePlayerSearchTask::ofPlatformThread
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
     * 已找到的物品数量
     */
    private final AtomicInteger itemCount = new AtomicInteger();
    /**
     * 在已找到的物品中，是否包含在嵌套的容器中找到的物品
     */
    private final AtomicBoolean shulkerBox = new AtomicBoolean(false);
    /**
     * 备份文件夹所在位置
     */
    private final WorldFormat worldFormat;
    private final FabricPlayerAccessManager accessManager;
    /**
     * 总的玩家数量
     */
    private int total = 0;
    protected final ServerPlayer player;
    private final MinecraftServer server;
    private final File[] files;
    private final ItemStackPredicate predicate;
    private State taksState = State.START;
    private final List<Result> results = Collections.synchronizedList(new ArrayList<>());
    /**
     * 备份文件夹的目录
     */
    private volatile WorldFormat backupFileDirectory;
    private final Object backupInitLock = new Object();
    private final PagedCollection pagedCollection;
    /**
     * 开始查找物品的时间
     */
    private long startTime;
    /**
     * 用于显示查找进度
     */
    @Nullable
    private ProgressBar progressBar;
    /**
     * 已经完成查找的人数
     */
    private final AtomicInteger completedCount = new AtomicInteger(0);
    public static final LocalizationKey KEY = ItemSearchTask.KEY.then("offline_player");
    private static final long PROGRESS_BAR_WAIT_TIME = 1000L;

    static {
        CPU_TASK_EXECUTOR.allowCoreThreadTimeOut(true);
    }

    public OfflinePlayerSearchTask(CommandSourceStack source, ItemStackPredicate predicate, ServerPlayer player) {
        super(source);
        this.predicate = predicate;
        this.player = player;
        this.server = ServerUtils.getServer(this.player);
        this.files = server.getWorldPath(LevelResource.PLAYER_DATA_DIR).toFile().listFiles();
        if (this.files == null) {
            throw new IllegalStateException("Unable to read \"playerdata\" folder");
        }
        this.worldFormat = new WorldFormat(this.server, "backups", "playerdata");
        PageManager manager = ServerComponentCoordinator.getCoordinator(this.server).getPageManager();
        this.pagedCollection = manager.newPagedCollection(this.source);
        this.accessManager = ServerComponentCoordinator.getCoordinator(server).getAccessManager();
    }

    @Override
    protected void tick() {
        switch (this.taksState) {
            case START -> {
                this.start();
                this.taksState = State.RUNTIME;
                this.startTime = System.currentTimeMillis();
            }
            case RUNTIME -> {
                LocalizationKey key = KEY.then("progress");
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
                    MessageUtils.sendMessageToHud(this.player, key.translate(this.predicate.getDisplayName(), this.progressBar.getDisplay()));
                }
            }
            case FEEDBACK -> {
                this.sendFeedback();
                this.taksState = State.STOP;
            }
            case STOP -> {
            }
        }
    }

    /**
     * 开始搜索物品
     */
    private void start() {
        for (File file : this.files) {
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
                if (INVALID_PLAYER_DATAS.contains(uuid)) {
                    return;
                }
                if (this.server.isRunning()) {
                    CompoundTag nbt = readNbt(unsafe, uuid);
                    if (nbt == null) {
                        return;
                    }
                    this.searchItem(uuid, nbt);
                }
            } catch (RuntimeException | IOException e) {
                CarpetOrgAddition.LOGGER.error("Unable to read player data from file for file {}", unsafe.getName(), e);
                addCorruptedPlayerUUID(uuid);
            } finally {
                this.taskCount.getAndDecrement();
                this.completedCount.getAndIncrement();
            }
        });
    }

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
            if (this.server.isRunning() && this.backupAndUpdate(unsafe, uuid)) {
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

    public static void clear() {
        CORRUPTED_PLAYER_DATAS.clear();
        BACKED_UP_FILES.clear();
        INVALID_PLAYER_DATAS.clear();
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
        Optional<NameAndId> optional = OfflinePlayerInventory.getPlayerConfigEntry(uuid, this.server);
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
        FabricPlayerAccessor accessor = this.accessManager.getOrCreateBlocking(entry);
        OfflinePlayerInventory inventory = new OfflinePlayerInventory(accessor);
        inventory.setShowLog(false);
        inventory.startOpen(this.player);
        inventory.stopOpen(this.player);
        return true;
    }

    private void backup(File file, UUID uuid) {
        // 这里只会数据更新时执行一次，多次执行则认为数据已经不可修复
        if (shouldBackup(file, uuid)) {
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

    // 查找物品
    private void searchItem(UUID uuid, @NotNull CompoundTag nbt) {
        // 获取玩家配置文件
        GameProfileCache cache = GameProfileCache.getInstance();
        Optional<NameAndId> optional = cache.getPlayerConfigEntry(uuid);
        boolean unknownPlayer = false;
        if (optional.isEmpty()) {
            optional = Optional.of(new NameAndId(uuid, UNKNOWN));
            unknownPlayer = true;
        }
        NameAndId entry = optional.get();
        // 不从在线玩家物品栏查找物品
        if (this.server.getPlayerList().getPlayerByName(entry.name()) != null) {
            return;
        }
        try {
            CURRENT_UUID.set(uuid);
            // 统计物品栏物品
            statistics(this.getInventory(nbt), entry, unknownPlayer, PlayerInventoryType.INVENTORY);
            statistics(this.getEnderChest(nbt), entry, unknownPlayer, PlayerInventoryType.ENDER_CHEST);
        } finally {
            CURRENT_UUID.remove();
        }
    }

    /**
     * 统计物品
     */
    private void statistics(Container inventory, NameAndId entry, boolean unknownPlayer, PlayerInventoryType type) {
        ItemStackStatistics statistics = new ItemStackStatistics(this.predicate);
        statistics.statistics(inventory);
        if (statistics.getSum() == 0) {
            return;
        }
        this.itemCount.addAndGet(statistics.getSum());
        if (statistics.hasNestingItem()) {
            this.shulkerBox.set(true);
        }
        Result result = new Result(entry, statistics, unknownPlayer, type, this.server);
        this.results.add(result);
    }

    // 获取玩家物品栏
    private Container getInventory(CompoundTag nbt) {
        return SimulatePlayerInventory.of(nbt, this.server);
    }

    /**
     * 从NBT读取玩家末影箱
     *
     * @see Player#readAdditionalSaveData(ValueInput)
     */
    @SuppressWarnings("JavadocReference")
    protected Container getEnderChest(CompoundTag nbt) {
        PlayerEnderChestContainer inventory = new PlayerEnderChestContainer();
        ValueInput readView = TagValueInput.create(ProblemReporter.DISCARDING, ServerUtils.getWorld(this.player).registryAccess(), nbt);
        inventory.fromSlots(readView.listOrEmpty("EnderItems", ItemStackWithSlot.CODEC));
        return inventory;
    }

    // 发送命令反馈
    private void sendFeedback() {
        if (this.results.isEmpty()) {
            MessageUtils.sendMessage(this.source, KEY.then("cannot_find").translate(this.predicate.getDisplayName()));
            return;
        }
        int resultCount = this.results.size();
        this.results.sort((o1, o2) -> o2.statistics().getSum() - o1.statistics().getSum());
        this.pagedCollection.addContent(this.results);
        Component itemCount = getItemCount();
        Component numberOfPeople = getNumberOfPeople(resultCount);
        Component message = getFirstFeedback(numberOfPeople, itemCount);
        TextBuilder builder = TextBuilder.of(message);
        builder.setHover(KEY.then("prompt").translate());
        MessageUtils.sendEmptyMessage(this.source);
        MessageUtils.sendMessage(this.source, builder.build());
        CommandUtils.handlingException(this.pagedCollection::print, source);
    }

    /**
     * 获取首条反馈消息
     */
    private Component getFirstFeedback(Component numberOfPeople, Component itemCount) {
        return KEY.then("head").translate(numberOfPeople, itemCount, this.predicate.getDisplayName());
    }

    /**
     * 获取物品数量文本
     */
    private Component getItemCount() {
        Optional<Item> optional = this.predicate.getConvert();
        if (optional.isPresent()) {
            return FinderCommand.showCount(optional.get().getDefaultInstance(), this.itemCount.get(), this.shulkerBox.get());
        } else {
            TextBuilder builder = TextBuilder.of(this.itemCount);
            return this.shulkerBox.get() ? builder.setItalic().build() : builder.build();
        }
    }

    /**
     * 获取玩家数量文本
     */
    private Component getNumberOfPeople(int resultCount) {
        // 玩家总数的悬停提示
        ArrayList<Component> peopleHover = new ArrayList<>();
        peopleHover.add(KEY.then("total").translate(this.total));
        peopleHover.add(KEY.then("found").translate(resultCount));
        TextBuilder builder = TextBuilder.of(resultCount);
        builder.setHover(TextBuilder.joinList(peopleHover));
        // 玩家总数文本
        return builder.build();
    }

    private Component getContainerName(PlayerInventoryType type) {
        TextBuilder builder = TextBuilder.of(type.getDisplayName());
        switch (type) {
            case INVENTORY -> builder.setColor(ChatFormatting.YELLOW);
            case ENDER_CHEST -> builder.setColor(ChatFormatting.DARK_PURPLE);
        }
        return builder.build();
    }

    @Override
    protected boolean stopped() {
        return this.taksState == State.STOP;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || this.getClass() == o.getClass();
    }

    @Override
    public int hashCode() {
        return 1;
    }

    /**
     * 添加打开玩家物品栏按钮
     */
    @Nullable
    protected Component openInventoryButton(NameAndId entry) {
        if (this.canOpenOfflinePlayerInventory(source)) {
            NbtWriter writer = new NbtWriter(this.server, CustomClickAction.CURRENT_VERSION);
            writer.putUuid(CustomClickKeys.UUID, entry.id());
            writer.putPlayerInventoryType(CustomClickKeys.INVENTORY_TYPE, PlayerInventoryType.INVENTORY);
            TextBuilder builder = TextBuilder.of("[O]");
            builder.setCustomEvent(CustomClickEvents.OPEN_INVENTORY, writer);
            builder.setHover(LocalizationKeys.Operation.OpenInventory.HOVER.translate(LocalizationKeys.Misc.INVENTORY.translate()));
            builder.setColor(ChatFormatting.GRAY);
            return builder.build();
        }
        return null;
    }

    @Nullable
    private Component openEnderChestButton(NameAndId entry) {
        if (this.canOpenOfflinePlayerInventory(source)) {
            NbtWriter writer = new NbtWriter(this.server, CustomClickAction.CURRENT_VERSION);
            writer.putUuid(CustomClickKeys.UUID, entry.id());
            writer.putPlayerInventoryType(CustomClickKeys.INVENTORY_TYPE, PlayerInventoryType.ENDER_CHEST);
            TextBuilder builder = TextBuilder.of("[O]");
            builder.setCustomEvent(CustomClickEvents.OPEN_INVENTORY, writer);
            builder.setHover(LocalizationKeys.Operation.OpenInventory.HOVER.translate(LocalizationKeys.Misc.ENDER_CHEST.translate()));
            builder.setColor(ChatFormatting.GRAY);
            return builder.build();
        }
        return null;
    }

    /**
     * @return 玩家是否可以打开离线玩家物品栏
     */
    private boolean canOpenOfflinePlayerInventory(CommandSourceStack source) {
        return CommandUtils.canUseCommand(source, CarpetSettings.commandPlayer)
               && OpenPlayerInventoryCommandOption.isEnable(source)
               && CarpetOrgAdditionSettings.playerCommandOpenPlayerInventoryOption.value().canOpenOfflinePlayer();
    }

    /**
     * @return 获取备份目录
     */
    private WorldFormat getBackupFileDirectory() {
        if (this.backupFileDirectory == null) {
            synchronized (this.backupInitLock) {
                String time = LocalDateTime.now().format(FORMATTER) + "_" + ServerUtils.getMinecraftDataVersion();
                this.backupFileDirectory = this.worldFormat.resolve(time);
            }
        }
        return this.backupFileDirectory;
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
     * @apiNote 非静态的内部类强引用了外部类导致暂时无法被回收，但这不是问题
     */
    public class Result implements Supplier<Component> {
        private final MinecraftServer server;
        private final NameAndId playerConfigEntry;
        private final ItemStackStatistics statistics;
        private final boolean isUnknown;
        private final PlayerInventoryType type;

        private Result(NameAndId playerConfigEntry, ItemStackStatistics statistics, boolean isUnknown, PlayerInventoryType type, MinecraftServer server) {
            this.playerConfigEntry = playerConfigEntry;
            this.statistics = statistics;
            this.isUnknown = isUnknown;
            this.type = type;
            this.server = server;
        }

        @Override
        public Component get() {
            // 获取玩家名，并添加UUID悬停提示
            String name = playerConfigEntry.name();
            String uuid = playerConfigEntry().id().toString();
            // 悬停提示
            Component hover = TextBuilder.combineAll("UUID: %s\n".formatted(uuid), TextProvider.COPY_CLICK);
            // 获取物品数量，如果包含在潜影盒中找到的物品，就设置物品为斜体
            Component count = statistics().getCountText();
            TextBuilder builder = getDisplayPlayerName(name, uuid, hover, count, this.type);
            return builder.build();
        }

        // 获取玩家显示名称
        private TextBuilder getDisplayPlayerName(String name, String uuid, Component hover, Component count, PlayerInventoryType type) {
            boolean unknown = isUnknown();
            TextBuilder builder = TextBuilder.of(unknown ? name : "[" + name + "]");
            if (unknown) {
                builder.setStrikethrough()
                        .setCopyToClipboard(uuid, false)
                        .append(createSearchButton());
            } else {
                builder.append(createLoginButton())
                        .setCopyToClipboard(name, false);
            }
            Component button = switch (type) {
                case INVENTORY -> openInventoryButton(this.playerConfigEntry());
                case ENDER_CHEST -> openEnderChestButton(this.playerConfigEntry());
            };
            builder.append(button)
                    .setHover(hover)
                    .setColor(ChatFormatting.GRAY);
            Component container = getContainerName(type);
            return KEY.then("each").builder(builder.build(), container, count);
        }

        // 创建单击上线按钮
        private Component createLoginButton() {
            if (CommandUtils.canUseCommand(source, CarpetSettings.commandPlayer)) {
                String command = CommandProvider.spawnFakePlayer(playerConfigEntry().name());
                TextBuilder builder = TextBuilder.of(" [↑]");
                builder.setCommand(command);
                builder.setHover(LocalizationKeys.Button.LOGIN.translate());
                return builder.build();
            }
            return TextBuilder.empty();
        }

        public NameAndId playerConfigEntry() {
            return playerConfigEntry;
        }

        // 创建查询玩家名称按钮
        private TextBuilder createSearchButton() {
            // 按钮的悬停提示
            ArrayList<Component> list = new ArrayList<>();
            list.add(LocalizationKeys.Operation.QueryPlayerName.Hover.FIRST.translate());
            list.add(TextBuilder.of(LocalizationKeys.Operation.QueryPlayerName.Hover.SECOND.translate()).setColor(ChatFormatting.RED).build());
            TextBuilder button = TextBuilder.of(" [\uD83D\uDD0D]");
            NbtWriter writer = new NbtWriter(this.server, CustomClickAction.CURRENT_VERSION);
            // 设置单击查询玩家名称
            writer.putUuid(CustomClickKeys.UUID, playerConfigEntry().id());
            button.setCustomEvent(CustomClickEvents.QUERY_PLAYER_NAME, writer);
            // 设置按钮悬停提示
            button.setHover(TextBuilder.joinList(list));
            return button;
        }

        public ItemStackStatistics statistics() {
            return statistics;
        }

        public boolean isUnknown() {
            return isUnknown;
        }
    }

    public enum State {
        START,
        RUNTIME,
        FEEDBACK,
        STOP
    }
}
