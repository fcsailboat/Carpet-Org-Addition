package org.carpetorgaddition.util.task.findtask;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.carpetorgaddition.command.FinderCommand;
import org.carpetorgaddition.exception.TaskExecutionException;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.task.ServerTask;
import org.carpetorgaddition.util.wheel.SelectionArea;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.StringJoiner;

public abstract class AbstractTradeFindTask extends ServerTask implements FindTask {
    protected final World world;
    protected final SelectionArea selectionArea;
    protected final BlockPos sourcePos;
    protected final CommandContext<ServerCommandSource> context;
    protected Iterator<MerchantEntity> iterator;
    private FindState findState;
    /**
     * tick方法开始执行时的时间
     */
    private long startTime;
    /**
     * 任务被执行的总游戏刻数
     */
    private int tickCount;
    protected final ArrayList<Result> results = new ArrayList<>();
    /**
     * 周围村民的数量，一般情况下等于this.results.size()，只在个别情况下例外，例如一个村民出售了至少3本相同附魔的附魔书，并且其中两本附魔书等级也相同
     */
    protected int villagerCount;
    /**
     * 交易选择的总数量
     */
    protected int tradeCount;

    public AbstractTradeFindTask(World world, SelectionArea selectionArea, BlockPos sourcePos, CommandContext<ServerCommandSource> context) {
        this.world = world;
        this.selectionArea = selectionArea;
        this.sourcePos = sourcePos;
        this.context = context;
        this.findState = FindState.SEARCH;
    }

    @Override
    public void tick() {
        this.startTime = System.currentTimeMillis();
        this.tickCount++;
        if (tickCount > FinderCommand.MAX_TICK_COUNT) {
            // 任务超时
            MessageUtils.sendErrorMessage(context, FinderCommand.TIME_OUT);
            this.findState = FindState.END;
            return;
        }
        while (true) {
            if (timeout()) {
                return;
            }
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
            this.iterator = this.world.getNonSpectatingEntities(MerchantEntity.class, this.selectionArea.toBox()).iterator();
        }
        while (this.iterator.hasNext()) {
            if (timeout()) {
                return;
            }
            MerchantEntity merchant = this.iterator.next();
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

    protected abstract void searchVillager(MerchantEntity merchant);

    protected abstract void notFound();

    protected abstract MutableText getTradeName();

    protected abstract String getResultLimitKey();

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
        String key;
        if (this.results.size() > FinderCommand.MAX_FEEDBACK_COUNT) {
            // 结果太多，限制只显示前10条
            key = getResultLimitKey();
            // 需要限制数量的消息中多一个占位符
            list.add(FinderCommand.MAX_FEEDBACK_COUNT);
        } else {
            key = "carpet.commands.finder.trade.result";
        }
        // 发送消息：在周围找到了<交易选项数量>个出售<出售的物品名称>的<村民>或<流浪商人>
        MessageUtils.sendMessage(context.getSource(), key, list.toArray(Object[]::new));
        // 发送每一条（或前10条）结果
        for (int i = 0; i < this.results.size() && i < FinderCommand.MAX_FEEDBACK_COUNT; i++) {
            MessageUtils.sendMessage(this.context.getSource(), this.results.get(i).toText());
        }
        this.findState = FindState.END;
    }

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

    // 当前任务是否超时
    private boolean timeout() {
        return (System.currentTimeMillis() - this.startTime) > FinderCommand.MAX_FIND_TIME;
    }

    public interface Result extends Comparator<Result> {
        MutableText toText();

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

    @Override
    public boolean taskExist(FindTask task) {
        if (this.getClass() == task.getClass()) {
            return this.context.getSource().getEntity() == ((AbstractTradeFindTask) task).context.getSource().getEntity();
        }
        return false;
    }

    private enum FindState {
        SEARCH, SORT, FEEDBACK, END
    }

    @Override
    public String getLogName() {
        return "交易查找";
    }
}
