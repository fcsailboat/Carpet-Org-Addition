package org.carpetorgaddition.periodic.task.findtask;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.command.FinderCommand;
import org.carpetorgaddition.exception.TaskExecutionException;
import org.carpetorgaddition.periodic.task.ServerTask;
import org.carpetorgaddition.util.MathUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.constant.TextConstants;
import org.carpetorgaddition.util.wheel.SelectionArea;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;

public class BlockFindTask extends ServerTask {
    protected final ServerWorld world;
    private final SelectionArea selectionArea;
    protected final CommandContext<ServerCommandSource> context;
    private final BlockPos sourcePos;
    private Iterator<BlockPos> iterator;
    private FindState findState;
    /**
     * tick方法开始执行时的时间
     */
    private long startTime;
    /**
     * 任务被执行的总游戏刻数
     */
    private int tickCount;
    private final FinderCommand.BlockPredicate blockPredicate;
    private final ArrayList<Result> results = new ArrayList<>();

    public BlockFindTask(ServerWorld world, BlockPos sourcePos, SelectionArea selectionArea, CommandContext<ServerCommandSource> context, FinderCommand.BlockPredicate blockPredicate) {
        this.world = world;
        this.sourcePos = sourcePos;
        this.selectionArea = selectionArea;
        this.context = context;
        this.blockPredicate = blockPredicate;
        this.findState = FindState.SEARCH;
        this.tickCount = 0;
    }

    @Override
    public void tick() {
        this.startTime = System.currentTimeMillis();
        this.tickCount++;
        if (this.tickCount > FinderCommand.MAX_TICK_COUNT) {
            // 任务超时
            MessageUtils.sendErrorMessage(context, FinderCommand.TIME_OUT);
            this.findState = FindState.END;
            return;
        }
        while (true) {
            if (this.timeout()) {
                return;
            }
            try {
                switch (this.findState) {
                    case SEARCH -> this.searchBlock();
                    case SORT -> this.sort();
                    case FEEDBACK -> this.sendFeedback();
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

    // 查找方块
    private void searchBlock() {
        if (this.iterator == null) {
            this.iterator = this.selectionArea.iterator();
        }
        while (this.iterator.hasNext()) {
            if (this.timeout()) {
                return;
            }
            BlockPos blockPos = this.iterator.next();
            // 获取区块XZ坐标
            int chunkX = ChunkSectionPos.getSectionCoord(blockPos.getX());
            int chunkZ = ChunkSectionPos.getSectionCoord(blockPos.getZ());
            // 判断区块是否已加载
            if (this.world.isChunkLoaded(chunkX, chunkZ)) {
                if (this.blockPredicate.test(world, blockPos)) {
                    this.results.add(new Result(this.sourcePos, blockPos));
                }
                if (this.results.size() > FinderCommand.MAXIMUM_STATISTICAL_COUNT) {
                    // 方块过多，无法统计
                    Runnable function = () -> MessageUtils.sendErrorMessage(
                            this.context,
                            "carpet.commands.finder.block.too_much_blocks",
                            this.blockPredicate.getName()
                    );
                    throw new TaskExecutionException(function);
                }
            }
        }
        this.findState = FindState.SORT;
    }

    // 对结果排序
    private void sort() {
        if (this.results.isEmpty()) {
            // 从周围没有找到指定方块
            MutableText name = this.blockPredicate.getName();
            MessageUtils.sendMessage(context.getSource(), "carpet.commands.finder.block.not_found_block", name);
            this.findState = FindState.END;
            return;
        }
        this.results.sort((o1, o2) -> MathUtils.compareBlockPos(this.sourcePos, o1.blockPos(), o2.blockPos()));
        this.findState = FindState.FEEDBACK;
    }

    // 发送反馈
    protected void sendFeedback() {
        int count = this.results.size();
        MutableText name = this.blockPredicate.getName();
        if (count <= CarpetOrgAdditionSettings.finderCommandMaxFeedbackCount) {
            MessageUtils.sendMessage(context.getSource(),
                    "carpet.commands.finder.block.find", count, name);
        } else {
            // 数量过多，只输出距离最近的前十个
            MessageUtils.sendMessage(context.getSource(), "carpet.commands.finder.block.find.limit",
                    count, name, CarpetOrgAdditionSettings.finderCommandMaxFeedbackCount);
        }
        for (int i = 0; i < this.results.size() && i < CarpetOrgAdditionSettings.finderCommandMaxFeedbackCount; i++) {
            MessageUtils.sendMessage(context.getSource(), this.results.get(i).toText());
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

    private class Result {
        private final BlockPos sourcteBlockPos;
        private final BlockPos blockPos;

        private Result(BlockPos sourcteBlockPos, BlockPos blockPos) {
            this.sourcteBlockPos = sourcteBlockPos;
            this.blockPos = blockPos;
        }

        public MutableText toText() {
            return getResultMessage(sourcteBlockPos, blockPos);
        }

        public BlockPos blockPos() {
            return blockPos;
        }
    }

    protected MutableText getResultMessage(BlockPos sourcteBlockPos, BlockPos blockPos) {
        return TextUtils.translate("carpet.commands.finder.block.feedback",
                MathUtils.getBlockIntegerDistance(sourcteBlockPos, blockPos),
                TextConstants.blockPos(blockPos, Formatting.GREEN));
    }

    @Override
    public boolean equals(Object obj) {
        if (this.getClass() == obj.getClass()) {
            return Objects.equals(this.context.getSource().getPlayer(), ((BlockFindTask) obj).context.getSource().getPlayer());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.context.getSource().getPlayer());
    }

    private enum FindState {
        SEARCH, SORT, FEEDBACK, END
    }

    @Override
    public String getLogName() {
        return "方块查找";
    }
}
