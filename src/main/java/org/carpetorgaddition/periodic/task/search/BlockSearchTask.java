package org.carpetorgaddition.periodic.task.search;

import net.minecraft.block.Block;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.carpetorgaddition.command.FinderCommand;
import org.carpetorgaddition.exception.ForceReturnException;
import org.carpetorgaddition.exception.TaskExecutionException;
import org.carpetorgaddition.periodic.task.ServerTask;
import org.carpetorgaddition.util.CommandUtils;
import org.carpetorgaddition.util.FetcherUtils;
import org.carpetorgaddition.util.MathUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.wheel.TextBuilder;
import org.carpetorgaddition.wheel.page.PageManager;
import org.carpetorgaddition.wheel.page.PagedCollection;
import org.carpetorgaddition.wheel.predicate.BlockStatePredicate;
import org.carpetorgaddition.wheel.provider.TextProvider;
import org.carpetorgaddition.wheel.traverser.BlockPosTraverser;

import java.util.*;
import java.util.function.Supplier;

public class BlockSearchTask extends ServerTask {
    protected final ServerWorld world;
    private final BlockPosTraverser traverser;
    protected final BlockPos sourcePos;
    private Iterator<BlockPos> iterator;
    private FindState findState;
    private final BlockStatePredicate predicate;
    /**
     * 已经迭代过的坐标集合
     */
    private final Set<BlockPos> blockPosCache = Collections.newSetFromMap(new WeakHashMap<>());
    /**
     * 找到的方块数量
     */
    private int count = 0;
    private final ArrayList<Result> results = new ArrayList<>();
    private final PagedCollection pagedCollection;

    public BlockSearchTask(ServerWorld world, BlockPos sourcePos, BlockPosTraverser traverser, ServerCommandSource source, BlockStatePredicate predicate) {
        super(source);
        this.world = world;
        this.sourcePos = sourcePos;
        this.traverser = traverser.clamp(world);
        this.predicate = predicate;
        this.findState = FindState.SEARCH;
        PageManager pageManager = FetcherUtils.getPageManager(source.getServer());
        this.pagedCollection = pageManager.newPagedCollection(this.source);
    }

    @Override
    public void tick() {
        this.checkTimeout();
        while (this.isTimeRemaining()) {
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
            this.iterator = this.traverser.iterator();
        }
        while (this.iterator.hasNext()) {
            if (this.isTimeExpired()) {
                return;
            }
            BlockPos blockPos = this.iterator.next();
            if (this.blockPosCache.contains(blockPos)) {
                continue;
            }
            try {
                this.iterate(blockPos);
            } catch (ForceReturnException e) {
                return;
            }
        }
        this.findState = FindState.SORT;
    }

    private void iterate(BlockPos begin) {
        HashMap<Block, Set<BlockPos>> group = new HashMap<>();
        BlockPos.iterateRecursively(begin, Integer.MAX_VALUE, Integer.MAX_VALUE, MathUtils::allDirection, blockPos -> {
            if (this.isTimeExpired()) {
                throw ForceReturnException.INSTANCE;
            }
            // 缓存方块坐标到软引用集合，下次不再迭代这个坐标
            if (this.predicate.test(this.world, blockPos) && this.blockPosCache.add(blockPos) && this.traverser.contains(blockPos)) {
                Block block = world.getBlockState(blockPos).getBlock();
                Set<BlockPos> set = group.computeIfAbsent(block, ignore -> new HashSet<>());
                // 如果软引用blockPosCache集合中的内容被回收，则此处可能重复执行
                if (set.add(blockPos)) {
                    this.count++;
                    if (this.count > FinderCommand.MAXIMUM_STATISTICAL_COUNT) {
                        // 方块过多，无法统计
                        Runnable function = () -> MessageUtils.sendErrorMessage(
                                this.source,
                                "carpet.commands.finder.block.too_much_blocks",
                                this.predicate.getDisplayName()
                        );
                        throw new TaskExecutionException(function);
                    }
                }
                return BlockPos.IterationState.ACCEPT;
            }
            return BlockPos.IterationState.STOP;
        });
        for (Map.Entry<Block, Set<BlockPos>> entry : group.entrySet()) {
            this.results.add(new Result(entry.getKey(), entry.getValue()));
        }
    }

    // 对结果排序
    private void sort() {
        if (this.results.isEmpty()) {
            // 从周围没有找到指定方块
            Text name = this.predicate.getDisplayName();
            MessageUtils.sendMessage(this.source, "carpet.commands.finder.block.not_found_block", name);
            this.findState = FindState.END;
            return;
        }
        this.results.sort((o1, o2) -> {
            int compare = Integer.compare(o1.set.size(), o2.set.size());
            if (compare == 0) {
                return MathUtils.compareBlockPos(this.sourcePos, o1.centerPos(), o2.centerPos());
            }
            return -compare;
        });
        this.findState = FindState.FEEDBACK;
    }

    // 发送反馈
    private void sendFeedback() {
        Text name = this.predicate.getDisplayName();
        MessageUtils.sendEmptyMessage(this.source);
        MessageUtils.sendMessage(this.source, "carpet.commands.finder.block.find", this.count, name);
        this.pagedCollection.addContent(this.results);
        CommandUtils.handlingException(this.pagedCollection::print, this.source);
        this.findState = FindState.END;
    }

    @Override
    public boolean stopped() {
        return this.findState == FindState.END;
    }

    private Text getResultMessage(Block block, Set<BlockPos> set) {
        BlockPos center = MathUtils.calculateTheGeometricCenter(set);
        TextBuilder builder = TextBuilder.of(
                "carpet.commands.finder.block.feedback",
                TextProvider.blockPos(center),
                set.size(),
                block.getName()
        );
        return builder.build();
    }

    @Override
    public boolean equals(Object obj) {
        if (this.getClass() == obj.getClass()) {
            return Objects.equals(this.source.getPlayer(), ((BlockSearchTask) obj).source.getPlayer());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.source.getPlayer());
    }

    @Override
    public String getLogName() {
        return "方块查找";
    }

    @Override
    public long getMaxExecutionTime() {
        return FinderCommand.MAX_SEARCH_TIME * 3;
    }

    @Override
    protected long getMaxTimeSlice() {
        return FinderCommand.TIME_SLICE;
    }

    public class Result implements Supplier<Text> {
        private final BlockPos center;
        private final Block block;
        private final Set<BlockPos> set;

        private Result(Block block, Set<BlockPos> set) {
            this.center = MathUtils.calculateTheGeometricCenter(set);
            this.block = block;
            this.set = set;
        }

        @Override
        public Text get() {
            return getResultMessage(this.block, this.set);
        }

        public BlockPos centerPos() {
            return center;
        }
    }

    public enum FindState {
        SEARCH,
        SORT,
        FEEDBACK,
        END
    }
}
