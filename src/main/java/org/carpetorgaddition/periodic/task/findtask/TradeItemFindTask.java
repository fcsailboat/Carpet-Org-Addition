package org.carpetorgaddition.periodic.task.findtask;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.TradeOfferList;
import net.minecraft.world.World;
import org.carpetorgaddition.command.FinderCommand;
import org.carpetorgaddition.util.MathUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.constant.TextConstants;
import org.carpetorgaddition.util.matcher.Matcher;
import org.carpetorgaddition.util.wheel.SelectionArea;

import java.util.ArrayList;

public class TradeItemFindTask extends AbstractTradeFindTask {
    private final Matcher matcher;
    private final MutableText treadName;

    public TradeItemFindTask(World world, SelectionArea selectionArea, BlockPos sourcePos, CommandContext<ServerCommandSource> context, Matcher matcher) {
        super(world, selectionArea, sourcePos, context);
        this.matcher = matcher;
        this.treadName = matcher.getName().copy();
    }

    @Override
    protected void searchVillager(MerchantEntity merchant) {
        TradeOfferList offers = merchant.getOffers();
        ArrayList<Integer> list = new ArrayList<>();
        for (int index = 0; index < offers.size(); index++) {
            // 检查每个出售的物品是否与匹配器匹配
            if (this.matcher.test(offers.get(index).getSellItem())) {
                list.add(index + 1);
                this.tradeCount++;
            }
        }
        if (list.isEmpty()) {
            return;
        }
        this.results.add(getResult(merchant, list));
        this.villagerCount++;
    }

    /**
     * @return 获取查找结果
     */
    private Result getResult(MerchantEntity merchant, ArrayList<Integer> list) {
        return new Result() {
            @Override
            public int compare(Result o1, Result o2) {
                return MathUtils.compareBlockPos(sourcePos, o1.villagerPos(), o2.villagerPos());
            }

            @Override
            public MutableText toText() {
                // 村民所在坐标
                BlockPos blockPos = merchant.getBlockPos();
                // 村民或流浪商人的名称
                MutableText villagerName = merchant.getName().copy();
                return TextUtils.translate("carpet.commands.finder.trade.item.each",
                        TextConstants.blockPos(blockPos, Formatting.GREEN), villagerName, getIndexArray(list));
            }

            @Override
            public BlockPos villagerPos() {
                return merchant.getBlockPos();
            }
        };
    }

    @Override
    protected void notFound() {
        MessageUtils.sendMessage(context.getSource(),
                "carpet.commands.finder.trade.find.not_trade",
                this.getTradeName(), FinderCommand.VILLAGER);
    }

    @Override
    protected MutableText getTradeName() {
        return this.treadName;
    }

    @Override
    protected String getResultLimitKey() {
        return "carpet.commands.finder.trade.result.limit";
    }
}
