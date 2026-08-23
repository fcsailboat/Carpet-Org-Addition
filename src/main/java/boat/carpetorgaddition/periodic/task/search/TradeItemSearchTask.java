package boat.carpetorgaddition.periodic.task.search;

import boat.carpetorgaddition.command.FinderCommand;
import boat.carpetorgaddition.util.MathUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.wheel.common.CommonTexts;
import boat.carpetorgaddition.wheel.predicate.ItemStackPredicate;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.traverser.BlockPosTraverser;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;

import java.util.ArrayList;

public class TradeItemSearchTask extends AbstractTradeSearchTask {
    private final ItemStackPredicate predicate;
    private final Component treadName;
    private static final LocalizationKey KEY = TRADE.then("item");

    public TradeItemSearchTask(Level world, BlockPosTraverser blockPosTraverser, BlockPos sourcePos, ItemStackPredicate predicate, CommandSourceStack source, ServerPlayer player) {
        super(world, blockPosTraverser, sourcePos, source, player);
        this.predicate = predicate;
        this.treadName = predicate.getDisplayName();
    }

    @Override
    protected void searchVillager(AbstractVillager merchant) {
        MerchantOffers offers = merchant.getOffers();
        ArrayList<Integer> list = new ArrayList<>();
        for (int index = 0; index < offers.size(); index++) {
            // 检查每个出售的物品是否与谓词匹配
            if (this.predicate.test(offers.get(index).getResult())) {
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
    private Result getResult(AbstractVillager merchant, ArrayList<Integer> list) {
        return new Result() {
            @Override
            public int compare(Result o1, Result o2) {
                return MathUtils.compareBlockPos(sourcePos, o1.villagerPos(), o2.villagerPos());
            }

            @Override
            public Component get() {
                // 村民所在坐标
                BlockPos blockPos = merchant.blockPosition();
                // 村民或流浪商人的名称
                Component villagerName = merchant.getName();
                Component pos = CommonTexts.blockPos(blockPos, ChatFormatting.GREEN);
                return KEY.then("each").translate(pos, villagerName, getIndexArray(list));
            }

            @Override
            public BlockPos villagerPos() {
                return merchant.blockPosition();
            }
        };
    }

    @Override
    protected void notFound() {
        MessageUtils.sendMessage(this.source, KEY.then("cannot_find").translate(this.getTradeName(), FinderCommand.VILLAGER));
    }

    @Override
    protected Component getTradeName() {
        return this.treadName;
    }

    @Override
    protected LocalizationKey getTradeResultKey() {
        return KEY.then("head");
    }
}
