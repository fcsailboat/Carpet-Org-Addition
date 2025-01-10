package org.carpetorgaddition.periodic.task.findtask;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.UserCache;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.command.FinderCommand;
import org.carpetorgaddition.periodic.task.ServerTask;
import org.carpetorgaddition.util.InventoryUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.inventory.ImmutableInventory;
import org.carpetorgaddition.util.inventory.SimulatePlayerInventory;
import org.carpetorgaddition.util.wheel.Counter;
import org.carpetorgaddition.util.wheel.ItemStackPredicate;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class OfflinePlayerFindTask extends ServerTask {
    private final AtomicInteger threadCount = new AtomicInteger();
    private final AtomicInteger itemCount = new AtomicInteger();
    private final AtomicBoolean shulkerBox = new AtomicBoolean(false);
    private final CommandContext<ServerCommandSource> context;
    private final UserCache userCache;
    protected final ServerPlayerEntity player;
    private final File[] files;
    private final ItemStackPredicate predicate;
    private State taksState = State.START;
    // synchronized会导致虚拟线程被锁定吗？还能不能使用并发集合？
    private final ReentrantLock lock = new ReentrantLock();
    private final ArrayList<Result> list = new ArrayList<>();

    public OfflinePlayerFindTask(
            CommandContext<ServerCommandSource> context,
            UserCache userCache,
            ServerPlayerEntity player,
            File[] files
    ) {
        this.context = context;
        this.predicate = new ItemStackPredicate(context, "itemStack");
        this.userCache = userCache;
        this.player = player;
        this.files = files;
    }

    @Override
    protected void tick() {
        switch (this.taksState) {
            case START -> {
                for (File file : files) {
                    if (file.getName().endsWith(".dat")) {
                        createVirtualThread(file);
                    }
                }
                this.taksState = State.RUNTIME;
            }
            case RUNTIME -> {
                if (this.threadCount.get() == 0) {
                    this.taksState = State.FEEDBACK;
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

    // 创建虚拟线程
    private void createVirtualThread(File file) {
        this.threadCount.getAndIncrement();
        Thread.ofVirtual().start(() -> {
            try {
                findItem(file);
            } finally {
                this.threadCount.getAndDecrement();
            }
        });
    }

    // 查找物品
    private void findItem(File file) {
        String uuid = file.getName().split("\\.")[0];
        // 获取玩家配置文件
        Optional<GameProfile> optional;
        try {
            optional = userCache.getByUuid(UUID.fromString(uuid));
        } catch (IllegalArgumentException e) {
            CarpetOrgAddition.LOGGER.warn("无法根据文件名{}解析UUID", file.getName(), e);
            return;
        }
        if (optional.isPresent()) {
            GameProfile gameProfile = optional.get();
            // 不从在线玩家物品栏查找物品
            if (this.player.server.getPlayerManager().getPlayer(gameProfile.getName()) != null) {
                return;
            }
            ArrayList<Result> results = new ArrayList<>();
            try {
                // 从玩家NBT读取物品栏
                NbtCompound nbt = NbtIo.readCompressed(file.toPath(), NbtSizeTracker.ofUnlimitedBytes());
                Inventory inventory = getInventory(nbt);
                // 统计物品栏物品
                count(results, inventory, gameProfile);
            } catch (IOException e) {
                CarpetOrgAddition.LOGGER.warn("无法从文件读取玩家数据：", e);
                return;
            }
            if (results.isEmpty()) {
                return;
            }
            try {
                this.lock.lock();
                this.list.addAll(results);
            } finally {
                this.lock.unlock();
            }
        }
    }

    // 获取玩家物品栏
    protected Inventory getInventory(NbtCompound nbt) {
        return SimulatePlayerInventory.of(nbt, this.player.getServer());
    }

    // 统计物品数量
    private void count(ArrayList<Result> results, Inventory inventory, GameProfile gameProfile) {
        Counter<Item> counter = new Counter<>();
        HashSet<Item> nesting = new HashSet<>();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack itemStack = inventory.getStack(i);
            if (itemStack.isEmpty()) {
                continue;
            }
            if (this.predicate.test(itemStack)) {
                counter.add(itemStack.getItem(), itemStack.getCount());
            } else if (InventoryUtils.isShulkerBoxItem(itemStack)) {
                ImmutableInventory shulkerBoxInventory = InventoryUtils.getInventory(itemStack);
                for (ItemStack stack : shulkerBoxInventory) {
                    if (this.predicate.test(stack)) {
                        counter.add(stack.getItem(), stack.getCount());
                        nesting.add(stack.getItem());
                    }
                }
            }
        }
        if (counter.isEmpty()) {
            return;
        }
        if (!nesting.isEmpty()) {
            this.shulkerBox.set(true);
        }
        for (Item item : counter) {
            int count = counter.getCount(item);
            this.itemCount.addAndGet(count);
            results.add(new Result(gameProfile, item, count, nesting.contains(item)));
        }
    }

    // 发送命令反馈
    private void sendFeedback() {
        if (this.list.isEmpty()) {
            MessageUtils.sendMessage(
                    this.context,
                    "carpet.commands.finder.item.offline_player.not_found",
                    this.getInventoryName(),
                    this.predicate.toText()
            );
            return;
        }
        this.list.sort((o1, o2) -> o2.count() - o1.count());
        MutableText hoverPrompt = TextUtils.translate(
                "carpet.commands.finder.item.offline_player.prompt",
                this.getInventoryName()
        );
        MutableText message;
        Text count;
        if (this.predicate.canConvertItem()) {
            count = FinderCommand.showCount(this.predicate.asItem().getDefaultStack(), this.itemCount.get(), this.shulkerBox.get());
        } else {
            count = TextUtils.createText(Integer.toString(this.itemCount.get()));
        }
        if (this.list.size() > FinderCommand.MAX_FEEDBACK_COUNT) {
            message = TextUtils.translate(
                    "carpet.commands.finder.item.offline_player.limit",
                    this.list.size(),
                    this.getInventoryName(),
                    count,
                    this.predicate.toText(),
                    FinderCommand.MAX_FEEDBACK_COUNT
            );
        } else {
            message = TextUtils.translate(
                    "carpet.commands.finder.item.offline_player",
                    this.list.size(),
                    this.getInventoryName(),
                    count,
                    this.predicate.toText()
            );
        }
        MessageUtils.sendMessage(this.context.getSource(), TextUtils.hoverText(message, hoverPrompt));
        for (int i = 0; i < Math.min(this.list.size(), FinderCommand.MAX_FEEDBACK_COUNT); i++) {
            Result result = this.list.get(i);
            sendEveryFeedback(result);
        }
    }

    // 发送每一条反馈
    private void sendEveryFeedback(Result result) {
        // 获取玩家名，并添加UUID悬停提示
        MutableText playerName = TextUtils.createText(result.gameProfile.getName());
        playerName = TextUtils.hoverText(playerName, TextUtils.createText("UUID:" + result.gameProfile.getId().toString()));
        playerName = TextUtils.setColor(playerName, Formatting.GRAY);
        // 获取物品数量，如果包含在潜影盒中找到的物品，就设置物品为斜体
        Text count = FinderCommand.showCount(result.item().getDefaultStack(), result.count, result.shulkerBox);
        MessageUtils.sendMessage(
                this.context,
                "carpet.commands.finder.item.offline_player.each",
                playerName,
                this.getInventoryName(),
                count
        );
    }

    protected Text getInventoryName() {
        return TextUtils.translate("container.inventory");
    }

    @Override
    protected boolean stopped() {
        return this.taksState == State.STOP;
    }

    @Override
    public String getLogName() {
        return "从离线玩家物品栏寻找物品";
    }

    @Override
    public boolean equals(Object o) {
        if (getClass() == o.getClass()) {
            return Objects.equals(player, ((OfflinePlayerFindTask) o).player);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(player);
    }

    private record Result(GameProfile gameProfile, Item item, int count, boolean shulkerBox) {
    }

    private enum State {
        START,
        RUNTIME,
        FEEDBACK,
        STOP
    }
}
