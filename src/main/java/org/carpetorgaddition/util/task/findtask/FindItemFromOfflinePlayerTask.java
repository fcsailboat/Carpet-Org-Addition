package org.carpetorgaddition.util.task.findtask;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.UserCache;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.carpetorgaddition.util.InventoryUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.task.ServerTask;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

public class FindItemFromOfflinePlayerTask extends ServerTask implements FindTask {
    // TODO 替换为原子整数
    private int threadCount = 0;
    private int itemCount = 0;
    private final CommandContext<ServerCommandSource> context;
    private final UserCache userCache;
    private final ServerPlayerEntity player;
    private final File[] files;
    private final Predicate<ItemStack> predicate;
    private final ItemStack itemStack;
    private State taksState = State.START;
    @Deprecated
    private final ReentrantLock countLock = new ReentrantLock();
    // synchronized会导致虚拟线程被锁定吗？
    private final ReentrantLock listLock = new ReentrantLock();
    private final ArrayList<Result> list = new ArrayList<>();

    public FindItemFromOfflinePlayerTask(CommandContext<ServerCommandSource> context, UserCache userCache, ServerPlayerEntity player, File[] files) throws CommandSyntaxException {
        this.context = context;
        ItemStack itemStack = ItemStackArgumentType.getItemStackArgument(context, "itemStack").createStack(1, false);
        this.itemStack = itemStack;
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
                try {
                    this.countLock.lock();
                    if (this.threadCount == 0) {
                        this.taksState = State.FEEDBACK;
                    }
                } finally {
                    this.countLock.unlock();
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

    private void createVirtualThread(File file) {
        try {
            this.countLock.lock();
            this.threadCount++;
        } finally {
            this.countLock.unlock();
        }
        Thread.ofVirtual().start(() -> {
            findItem(file);
            try {
                this.countLock.lock();
                this.threadCount--;
            } finally {
                this.countLock.unlock();
            }
        });
    }

    private void findItem(File file) {
        String uuid = file.getName().split("\\.")[0];
        // TODO UUID.fromString(uuid)可能抛出方法参数异常
        Optional<GameProfile> optional = userCache.getByUuid(UUID.fromString(uuid));
        if (optional.isPresent()) {
            Result result;
            try {
                result = count(player.getServerWorld(), optional.get());
            } catch (IOException e) {
                // TODO 处理异常
                return;
            }
            if (result == null) {
                return;
            }
            try {
                this.listLock.lock();
                this.list.add(result);
            } finally {
                this.listLock.unlock();
            }
        }
    }

    @Nullable
    private Result count(ServerWorld world, GameProfile gameProfile) throws IOException {
        FakePlayer fakePlayer = FakePlayer.get(world, gameProfile);
        world.getServer().getPlayerManager().loadPlayerData(fakePlayer);
        PlayerInventory inventory = fakePlayer.getInventory();
        MutableInt count = new MutableInt();
        MutableBoolean bool = new MutableBoolean(false);
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack itemStack = inventory.getStack(i);
            if (itemStack.isEmpty()) {
                continue;
            }
            if (this.predicate.test(itemStack)) {
                count.add(itemStack.getCount());
            } else if (InventoryUtils.isShulkerBoxItem(itemStack)) {
                InventoryUtils.shulkerBoxConsumer(itemStack, this.predicate, stack -> {
                    count.add(itemStack.getCount());
                    bool.setTrue();
                });
            }
        }
        if (count.getValue() == 0) {
            return null;
        }
        try {
            this.listLock.lock();
            this.itemCount += count.getValue();
        } finally {
            this.listLock.unlock();
        }
        return new Result(fakePlayer.getName().getString(), count.getValue(), bool.getValue());
    }

    private void sendFeedback() {
        MessageUtils.sendMessage(this.context, "在" + this.list.size() + "个玩家身上找到了" + this.itemCount + "个" + itemStack.getItem().getName().getString() + "：");
        for (Result result : this.list) {
            MessageUtils.sendMessage(this.context, "在" + result.playerName + "身上找到了" + result.count + "个");
        }
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
        return false;
    }

    private record Result(String playerName, int count, boolean shulkerBox) {
    }

    private enum State {
        START,
        RUNTIME,
        FEEDBACK,
        STOP
    }
}
