package boat.carpetorgaddition.periodic.task.search;

import boat.carpetorgaddition.command.FinderCommand;
import boat.carpetorgaddition.exception.ForceReturnException;
import boat.carpetorgaddition.exception.TaskExecutionException;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.util.MathUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.wheel.ProgressBar;
import boat.carpetorgaddition.wheel.page.PageManager;
import boat.carpetorgaddition.wheel.page.PagedCollection;
import boat.carpetorgaddition.wheel.predicate.BlockStatePredicate;
import boat.carpetorgaddition.wheel.provider.TextProvider;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.traverser.BlockPosTraverser;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;

import java.util.*;
import java.util.function.Supplier;

public class BlockSearchTask extends ServerSearchTask {
    protected final ServerLevel world;
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
    private final ProgressBar progressBar;
    /**
     * 已遍历过的方块数量
     */
    private int progress = 0;
    /**
     * 当前任务是否需要停止
     */
    private boolean cancelled = false;
    public static final LocalizationKey KEY = FinderCommand.KEY.then("block");

    public BlockSearchTask(ServerLevel world, BlockPos sourcePos, BlockPosTraverser traverser, CommandSourceStack source, BlockStatePredicate predicate, ServerPlayer player) {
        super(source, player);
        this.world = world;
        this.sourcePos = sourcePos;
        this.traverser = traverser.clamp(world);
        this.progressBar = new ProgressBar(this.traverser.size());
        this.predicate = predicate;
        this.findState = FindState.SEARCH;
        PageManager pageManager = ServerComponentCoordinator.getCoordinator(source.getServer()).getPageManager();
        this.pagedCollection = pageManager.newPagedCollection(this.source);
    }

    @Override
    public void tick() {
        if (this.isTimeout()) {
            this.noticeCancelled();
        }
        this.checkCancelled();
        while (this.isTimeRemaining()) {
            switch (this.findState) {
                case SEARCH -> {
                    this.searchBlock();
                    MessageUtils.sendMessageToHudIfPlayer(this.source, () -> KEY
                            .then("progress")
                            .translate(this.predicate.getDisplayName(), this.progressBar.getDisplay())
                    );
                }
                case SORT -> this.sort();
                case FEEDBACK -> this.sendFeedback();
                default -> {
                    return;
                }
            }
        }
    }

    // 查找方块
    private void searchBlock() {
        if (this.iterator == null) {
            this.iterator = this.traverser.iterator();
        }
        while (this.iterator.hasNext()) {
            this.checkCancelled();
            if (this.isTimeExpired()) {
                return;
            }
            BlockPos blockPos = this.iterator.next();
            if (this.blockPosCache.contains(blockPos)) {
                continue;
            }
            try {
                this.iterate(blockPos);
                this.progress++;
                this.progressBar.setProgress(this.progress);
            } catch (ForceReturnException e) {
                return;
            }
        }
        this.findState = FindState.SORT;
        this.progressBar.setCompleted();
    }

    private void iterate(BlockPos begin) {
        HashMap<Block, Set<BlockPos>> group = new HashMap<>();
        BlockPos.breadthFirstTraversal(begin, Integer.MAX_VALUE, Integer.MAX_VALUE, MathUtils::allDirection, blockPos -> {
            this.checkCancelled();
            if (this.isTimeExpired()) {
                throw ForceReturnException.INSTANCE;
            }
            // 缓存方块坐标到软引用集合，下次不再迭代这个坐标
            if (this.predicate.test(this.world, blockPos) && this.blockPosCache.add(blockPos) && this.traverser.contains(blockPos)) {
                Block block = world.getBlockState(blockPos).getBlock();
                Set<BlockPos> set = group.computeIfAbsent(block, _ -> new HashSet<>());
                // 如果软引用blockPosCache集合中的内容被回收，则此处可能重复执行
                if (set.add(blockPos)) {
                    this.count++;
                    if (this.count > FinderCommand.MAXIMUM_STATISTICAL_COUNT) {
                        // 方块过多，无法统计
                        throw new TaskExecutionException(
                                () -> MessageUtils.sendErrorMessage(
                                        this.source,
                                        KEY.then("too_much").translate(this.predicate.getDisplayName())
                                )
                        );
                    }
                }
                return BlockPos.TraversalNodeStatus.ACCEPT;
            }
            return BlockPos.TraversalNodeStatus.SKIP;
        });
        for (Map.Entry<Block, Set<BlockPos>> entry : group.entrySet()) {
            this.results.add(new Result(entry.getKey(), entry.getValue()));
        }
    }

    // 对结果排序
    private void sort() {
        if (this.results.isEmpty()) {
            // 从周围没有找到指定方块
            Component name = this.predicate.getDisplayName();
            MessageUtils.sendMessage(this.source, KEY.then("cannot_find").translate(name));
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
        Component name = this.predicate.getDisplayName();
        MessageUtils.sendEmptyMessage(this.source);
        MessageUtils.sendMessage(this.source, KEY.then("head").translate(this.count, name));
        this.pagedCollection.addContent(this.results);
        CommandUtils.handlingException(this.pagedCollection::print, this.source);
        this.findState = FindState.END;
    }

    @Override
    public boolean stopped() {
        return this.findState == FindState.END;
    }

    private Component getResultMessage(Block block, Set<BlockPos> set) {
        BlockPos center = MathUtils.calculateTheGeometricCenter(set);
        return KEY.then("each").translate(TextProvider.blockPos(center), set.size(), block.getName());
    }

    @Override
    public void cancel() {
        this.cancelled = true;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public long getMaxExecutionTime() {
        return FinderCommand.MAX_SEARCH_TIME * 3;
    }

    @Override
    protected long getMaxTimeSlice() {
        return FinderCommand.TIME_SLICE;
    }

    public class Result implements Supplier<Component> {
        private final BlockPos center;
        private final Block block;
        private final Set<BlockPos> set;

        private Result(Block block, Set<BlockPos> set) {
            this.center = MathUtils.calculateTheGeometricCenter(set);
            this.block = block;
            this.set = set;
        }

        @Override
        public Component get() {
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
