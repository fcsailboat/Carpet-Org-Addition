package boat.carpetorgaddition.periodic.task.search;

import boat.carpetorgaddition.command.FinderCommand;
import boat.carpetorgaddition.exception.TaskExecutionException;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.page.PageManager;
import boat.carpetorgaddition.wheel.page.PagedCollection;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.traverser.BlockPosTraverser;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.StringJoiner;
import java.util.function.Supplier;

public abstract class AbstractTradeSearchTask extends ServerSearchTask {
    protected final Level world;
    protected final BlockPosTraverser blockPosTraverser;
    protected final BlockPos sourcePos;
    protected Iterator<AbstractVillager> iterator;
    private final PagedCollection pagedCollection;
    private FindState findState;
    protected final ArrayList<Result> results = new ArrayList<>();
    /**
     * 周围村民的数量，一般情况下等于this.results.size()，只在个别情况下例外，例如一个村民出售了至少3本相同附魔的附魔书，并且其中两本附魔书等级也相同
     */
    protected int villagerCount;
    /**
     * 交易选择的总数量
     */
    protected int tradeCount;
    public static final LocalizationKey TRADE = FinderCommand.KEY.then("trade");

    public AbstractTradeSearchTask(Level world, BlockPosTraverser blockPosTraverser, BlockPos sourcePos, CommandSourceStack source) {
        super(source);
        this.world = world;
        this.blockPosTraverser = blockPosTraverser;
        this.sourcePos = sourcePos;
        this.findState = FindState.SEARCH;
        PageManager pageManager = ServerComponentCoordinator.getCoordinator(ServerUtils.getServer(source)).getPageManager();
        this.pagedCollection = pageManager.newPagedCollection(this.source);
    }

    @Override
    public void tick() {
        this.checkTimeout();
        while (this.isTimeRemaining()) {
            try {
                switch (this.findState) {
                    case SEARCH -> searchVillager();
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

    // 查找周围的村民
    private void searchVillager() {
        if (this.iterator == null) {
            this.iterator = this.world.getEntitiesOfClass(AbstractVillager.class, this.blockPosTraverser.toBox()).iterator();
        }
        while (this.iterator.hasNext()) {
            if (this.isTimeExpired()) {
                return;
            }
            AbstractVillager merchant = this.iterator.next();
            // 检查每一只村民交易
            this.searchVillager(merchant);
        }
        // 没有找到交易物品，跳过排序直接结束任务
        if (this.results.isEmpty()) {
            this.finish();
            return;
        }
        this.findState = FindState.SORT;
    }

    protected abstract void searchVillager(AbstractVillager merchant);

    protected abstract void notFound();

    protected abstract Component getTradeName();

    // 对结果进行排序
    private void sort() {
        this.results.sort((o1, o2) -> o1.compare(o1, o2));
        this.findState = FindState.FEEDBACK;
    }

    private void feedback() {
        ArrayList<Object> list = new ArrayList<>();
        // 村民数量
        list.add(this.villagerCount);
        list.add(this.getTradeName());
        list.add(FinderCommand.VILLAGER);
        // 总交易选项数量
        list.add(this.tradeCount);
        // 消息的翻译键
        LocalizationKey key = this.getTradeResultKey();
        MessageUtils.sendEmptyMessage(this.source);
        // 发送消息：在周围找到了<交易选项数量>个出售<出售的物品名称>的<村民>或<流浪商人>
        MessageUtils.sendMessage(this.source, key.translate(list.toArray(Object[]::new)));
        this.pagedCollection.addContent(this.results);
        CommandUtils.handlingException(this.pagedCollection::print, this.source);
        this.findState = FindState.END;
    }

    protected abstract LocalizationKey getTradeResultKey();

    @Override
    public boolean stopped() {
        return this.findState == FindState.END;
    }

    /**
     * 因没找到交易物品而停止查找
     */
    private void finish() {
        this.notFound();
        this.findState = FindState.END;
    }

    @Override
    public long getMaxExecutionTime() {
        return FinderCommand.MAX_SEARCH_TIME;
    }

    @Override
    protected long getMaxTimeSlice() {
        return FinderCommand.TIME_SLICE;
    }

    public interface Result extends Comparator<Result>, Supplier<Component> {
        BlockPos villagerPos();
    }

    /**
     * @param list 一只村民所有符合条件交易的索引
     * @return 索引拼接后的字符串
     */
    protected static String getIndexArray(ArrayList<Integer> list) {
        String indexArray;
        // 如果只有一个索引，直接返回元素字符串
        if (list.size() == 1) {
            return list.getFirst().toString();
        }
        // 如果多个索引，将索引拼接后返回
        StringJoiner stringJoiner = new StringJoiner(", ", "[", "]");
        for (Integer index : list) {
            stringJoiner.add(index.toString());
        }
        indexArray = stringJoiner.toString();
        return indexArray;
    }

    public enum FindState {
        SEARCH,
        SORT,
        FEEDBACK,
        END
    }
}
