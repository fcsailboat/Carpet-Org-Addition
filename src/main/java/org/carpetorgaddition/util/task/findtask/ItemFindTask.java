package org.carpetorgaddition.util.task.findtask;

import carpet.patches.EntityPlayerMPFake;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.vehicle.VehicleInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.carpetorgaddition.command.FinderCommand;
import org.carpetorgaddition.exception.TaskExecutionException;
import org.carpetorgaddition.util.InventoryUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.constant.TextConstants;
import org.carpetorgaddition.util.inventory.ImmutableInventory;
import org.carpetorgaddition.util.matcher.Matcher;
import org.carpetorgaddition.util.task.ServerTask;
import org.carpetorgaddition.util.wheel.Counter;
import org.carpetorgaddition.util.wheel.SelectionArea;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

public class ItemFindTask extends ServerTask implements FindTask {
    private final World world;
    private final SelectionArea selectionArea;
    private final CommandContext<ServerCommandSource> context;
    private Iterator<Entity> entitySearchIterator;
    private Iterator<BlockPos> blockSearchIterator;
    private FindState findState;
    private int count;
    private boolean shulkerBox;
    /**
     * tick方法开始执行时的时间
     */
    private long startTime;
    /**
     * 任务被执行的总游戏刻数
     */
    private int tickCount;
    private final Matcher matcher;
    private final ArrayList<Result> results = new ArrayList<>();

    public ItemFindTask(World world, Matcher matcher, SelectionArea selectionArea, CommandContext<ServerCommandSource> context) {
        this.world = world;
        this.selectionArea = selectionArea;
        this.findState = FindState.BLOCK;
        this.tickCount = 0;
        this.matcher = matcher;
        this.context = context;
    }

    @Override
    public void tick() {
        this.startTime = System.currentTimeMillis();
        this.tickCount++;
        if (tickCount > FinderCommand.MAX_TICK_COUNT) {
            // 任务超时
            MessageUtils.sendCommandErrorFeedback(context, FinderCommand.TIME_OUT);
            this.findState = FindState.END;
            return;
        }
        while (true) {
            if (timeout()) {
                return;
            }
            try {
                switch (this.findState) {
                    case BLOCK -> searchFromContainer();
                    case ENTITY -> searchFromEntity();
                    case SORT -> sort();
                    case FEEDBACK -> feedback();
                    default -> {
                        return;
                    }
                }
            } catch (TaskExecutionException e) {
                e.disposal();
                this.findState = FindState.END;
                return;
            }
        }
    }

    // 从容器查找
    private void searchFromContainer() {
        if (this.blockSearchIterator == null) {
            this.blockSearchIterator = selectionArea.iterator();
        }
        while (this.blockSearchIterator.hasNext()) {
            if (this.timeout()) {
                return;
            }
            BlockPos blockPos = this.blockSearchIterator.next();
            BlockEntity blockEntity = this.world.getBlockEntity(blockPos);
            if (blockEntity instanceof Inventory inventory) {
                // 获取容器名称
                MutableText containerName
                        = inventory instanceof LockableContainerBlockEntity lockableContainer
                        ? lockableContainer.getName().copy()
                        : this.world.getBlockState(blockPos).getBlock().getName();
                this.count(inventory, null, blockPos, containerName);
            }
        }
        this.findState = FindState.ENTITY;
    }

    // 从实体查找
    private void searchFromEntity() {
        Box box = selectionArea.toBox();
        if (this.entitySearchIterator == null) {
            this.entitySearchIterator = this.world.getNonSpectatingEntities(Entity.class, box).iterator();
        }
        while (this.entitySearchIterator.hasNext()) {
            if (timeout()) {
                return;
            }
            Entity entity = this.entitySearchIterator.next();
            // 掉落物
            if (entity instanceof ItemEntity itemEntity) {
                this.count(new SimpleInventory(itemEntity.getStack()), entity.getUuid(), itemEntity.getBlockPos(),
                        TextUtils.translate("carpet.commands.finder.item.drops"));
                continue;
            }
            // 假玩家
            if (entity instanceof EntityPlayerMPFake fakePlayer) {
                this.count(fakePlayer.getInventory(), entity.getUuid(), fakePlayer.getBlockPos(), fakePlayer.getName().copy());
                continue;
            }
            // 容器实体
            if (entity instanceof VehicleInventory inventory) {
                this.count(inventory, entity.getUuid(), entity.getBlockPos(), entity.getName().copy());
            }
        }
        this.findState = FindState.SORT;
    }

    // 统计符合条件的物品
    private void count(Inventory inventory, @Nullable UUID uuid, BlockPos blockPos, MutableText containerName) {
        // 是否有物品是在潜影盒中找到的
        boolean shulkerBox = false;
        Counter<Item> counter = new Counter<>();
        for (int index = 0; index < inventory.size(); index++) {
            ItemStack itemStack = inventory.getStack(index);
            if (this.matcher.test(itemStack)) {
                // 统计符合条件的物品数量
                counter.add(itemStack.getItem(), itemStack.getCount());
            } else if (InventoryUtils.isShulkerBoxItem(itemStack)) {
                ImmutableInventory immutableInventory = InventoryUtils.getInventory(itemStack);
                for (ItemStack stack : immutableInventory) {
                    if (this.matcher.test(stack)) {
                        counter.add(stack.getItem(), stack.getCount());
                        shulkerBox = true;
                    }
                }
            }
        }
        if (this.results.size() > FinderCommand.MAXIMUM_STATISTICAL_COUNT) {
            // 容器太多，无法统计
            Runnable function = () -> MessageUtils.sendCommandErrorFeedback(context,
                    "carpet.commands.finder.item.too_much_container",
                    this.matcher.toText());
            throw new TaskExecutionException(function);
        }
        // 如果为物品标签，那么同一个容器中可能出现多中匹配的物品
        for (Item item : counter) {
            int count = counter.getCount(item);
            this.count += count;
            if (shulkerBox) {
                this.shulkerBox = true;
            }
            this.results.add(new Result(item, uuid, blockPos, containerName, count, shulkerBox));
        }
    }

    // 对结果进行排序
    private void sort() {
        if (this.results.isEmpty()) {
            // 在周围的容器中找不到指定物品，直接将状态设置为结束，然后结束方法
            MessageUtils.sendCommandFeedback(context.getSource(), "carpet.commands.finder.item.find.not_item", matcher.toText());
            this.findState = FindState.END;
            return;
        }
        this.results.sort((o1, o2) -> o2.count() - o1.count());
        this.findState = FindState.FEEDBACK;
    }

    // 发送命令反馈
    private void feedback() {
        MutableText itemOrTagName;
        boolean isItem = matcher.isItem();
        if (isItem) {
            // 为数量添加鼠标悬停效果
            itemOrTagName = FinderCommand.showCount(matcher.getItem().getDefaultStack(), this.count, this.shulkerBox);
        } else {
            itemOrTagName = TextUtils.regularStyle(String.valueOf(count), null, false, this.shulkerBox, false, false);
        }
        if (this.results.size() <= FinderCommand.MAX_FEEDBACK_COUNT) {
            MessageUtils.sendCommandFeedback(context.getSource(), "carpet.commands.finder.item.find",
                    this.results.size(), itemOrTagName, matcher.toText());
        } else {
            MessageUtils.sendCommandFeedback(context.getSource(), "carpet.commands.finder.item.find.limit",
                    this.results.size(), itemOrTagName, matcher.toText(), FinderCommand.MAX_FEEDBACK_COUNT);
        }
        for (int i = 0; i < this.results.size() && i <= FinderCommand.MAX_FEEDBACK_COUNT; i++) {
            MutableText message = this.results.get(i).toText();
            if (isItem) {
                MessageUtils.sendTextMessage(this.context.getSource(), message);
            } else {
                MessageUtils.sendTextMessage(this.context.getSource(), TextUtils.appendAll(message, this.results.get(i).item().getName().copy()));
            }
        }
        this.findState = FindState.END;
    }

    // 当前任务是否超时
    private boolean timeout() {
        return (System.currentTimeMillis() - this.startTime) > FinderCommand.MAX_FIND_TIME;
    }

    @Override
    public boolean stopped() {
        return this.findState == FindState.END;
    }

    @Override
    public boolean taskExist(FindTask task) {
        if (this.getClass() == task.getClass()) {
            return this.context.getSource().getEntity() == ((ItemFindTask) task).context.getSource().getEntity();
        }
        return false;
    }

    private record Result(Item item, @Nullable UUID uuid, BlockPos blockPos,
                          MutableText containerName, int count, boolean shulkerBox) {
        private MutableText toText() {
            return TextUtils.translate("carpet.commands.finder.item.each",
                    TextConstants.blockPos(blockPos, Formatting.GREEN),
                    containerName,
                    FinderCommand.showCount(item.getDefaultStack(), count, shulkerBox));
        }
    }

    private enum FindState {
        BLOCK, ENTITY, SORT, FEEDBACK, END
    }

    @Override
    public String getLogName() {
        return "物品查找";
    }
}
