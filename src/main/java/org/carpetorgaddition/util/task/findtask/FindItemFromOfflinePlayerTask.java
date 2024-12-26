package org.carpetorgaddition.util.task.findtask;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.inventory.Inventory;
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
import org.carpetorgaddition.util.InventoryUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.inventory.ImmutableInventory;
import org.carpetorgaddition.util.inventory.SimulatePlayerInventory;
import org.carpetorgaddition.util.task.ServerTask;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

public class FindItemFromOfflinePlayerTask extends ServerTask implements FindTask {
    private final AtomicInteger threadCount = new AtomicInteger();
    private final AtomicInteger itemCount = new AtomicInteger();
    private final AtomicBoolean shulkerBox = new AtomicBoolean(false);
    private final CommandContext<ServerCommandSource> context;
    private final UserCache userCache;
    private final ServerPlayerEntity player;
    private final File[] files;
    private final Predicate<ItemStack> predicate;
    private final ItemStack targetStack;
    private State taksState = State.START;
    // synchronized会导致虚拟线程被锁定吗？还能不能使用并发集合？
    private final ReentrantLock lock = new ReentrantLock();
    private final ArrayList<Result> list = new ArrayList<>();

    public FindItemFromOfflinePlayerTask(
            CommandContext<ServerCommandSource> context,
            UserCache userCache,
            ServerPlayerEntity player,
            File[] files
    ) throws CommandSyntaxException {
        this.context = context;
        ItemStack itemStack = ItemStackArgumentType
                .getItemStackArgument(context, "itemStack")
                .createStack(1, false);
        this.targetStack = itemStack;
        this.predicate = stack -> ItemStack.areItemsAndComponentsEqual(itemStack, stack);
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
            Result result;
            try {
                // 从玩家NBT读取物品栏
                NbtCompound nbt = NbtIo.readCompressed(file.toPath(), NbtSizeTracker.ofUnlimitedBytes());
                SimulatePlayerInventory inventory = SimulatePlayerInventory.of(nbt, this.player.getServer());
                // 统计物品栏物品
                result = count(inventory, gameProfile);
            } catch (IOException e) {
                CarpetOrgAddition.LOGGER.warn("无法从文件读取玩家数据：", e);
                return;
            }
            if (result == null) {
                return;
            }
            try {
                this.lock.lock();
                this.list.add(result);
            } finally {
                this.lock.unlock();
            }
        }
    }

    // 统计物品数量
    @Nullable
    private Result count(Inventory inventory, GameProfile gameProfile) {
        int count = 0;
        boolean inShulkerBox = false;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack itemStack = inventory.getStack(i);
            if (itemStack.isEmpty()) {
                continue;
            }
            if (this.predicate.test(itemStack)) {
                count += itemStack.getCount();
            } else if (InventoryUtils.isShulkerBoxItem(itemStack)) {
                ImmutableInventory shulkerBoxInventory = InventoryUtils.getInventory(itemStack);
                for (ItemStack stack : shulkerBoxInventory) {
                    if (this.predicate.test(stack)) {
                        count += stack.getCount();
                        inShulkerBox = true;
                    }
                }
            }
        }
        if (count == 0) {
            return null;
        }
        this.itemCount.addAndGet(count);
        if (inShulkerBox) {
            this.shulkerBox.set(true);
        }
        return new Result(gameProfile, count, inShulkerBox);
    }

    // 发送命令反馈
    private void sendFeedback() {
        if (this.list.isEmpty()) {
            MessageUtils.sendMessage(
                    this.context,
                    "carpet.commands.finder.item.offline_player.not_found",
                    this.targetStack.getItem().getName()
            );
            return;
        }
        this.list.sort((o1, o2) -> o2.count() - o1.count());
        MutableText hoverPrompt = TextUtils.translate("carpet.commands.finder.item.offline_player.prompt");
        MutableText message;
        Text count = FinderCommand.showCount(this.targetStack, this.itemCount.get(), this.shulkerBox.get());
        if (this.list.size() > FinderCommand.MAX_FEEDBACK_COUNT) {
            message = TextUtils.translate(
                    "carpet.commands.finder.item.offline_player.limit",
                    this.list.size(),
                    count,
                    this.targetStack.getItem().getName(),
                    FinderCommand.MAX_FEEDBACK_COUNT
            );
        } else {
            message = TextUtils.translate(
                    "carpet.commands.finder.item.offline_player",
                    this.list.size(),
                    count,
                    this.targetStack.getItem().getName()
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
        Text count = FinderCommand.showCount(this.targetStack, result.count, result.shulkerBox);
        MessageUtils.sendMessage(this.context, "carpet.commands.finder.item.offline_player.each", playerName, count);
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
    public boolean taskExist(FindTask task) {
        if (this.getClass() == task.getClass()) {
            return this.context.getSource().getEntity() == ((FindItemFromOfflinePlayerTask) task).context.getSource().getEntity();
        }
        return false;
    }

    private record Result(GameProfile gameProfile, int count, boolean shulkerBox) {
    }

    private enum State {
        START,
        RUNTIME,
        FEEDBACK,
        STOP
    }
}
