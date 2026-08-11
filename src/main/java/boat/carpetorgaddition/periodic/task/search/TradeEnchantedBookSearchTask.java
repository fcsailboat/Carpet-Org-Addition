package boat.carpetorgaddition.periodic.task.search;

import boat.carpetorgaddition.command.FinderCommand;
import boat.carpetorgaddition.util.MathUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.wheel.predicate.EnchantedBookPredicate;
import boat.carpetorgaddition.wheel.provider.TextProvider;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.traverser.BlockPosTraverser;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TradeEnchantedBookSearchTask extends AbstractTradeSearchTask {
    private final EnchantedBookPredicate predicate;
    private static final LocalizationKey KEY = TRADE.then("enchanted_book");

    public TradeEnchantedBookSearchTask(Level world, BlockPosTraverser blockPosTraverser, BlockPos sourcePos, CommandSourceStack source, EnchantedBookPredicate predicate, ServerPlayer player) {
        super(world, blockPosTraverser, sourcePos, source, player);
        this.predicate = predicate;
    }

    @Override
    protected void searchVillager(AbstractVillager merchant) {
        MerchantOffers offers = merchant.getOffers();
        // 键：附魔书的等级，值：同一只村民出售的相同附魔书的索引集合
        HashMap<Integer, ArrayList<Integer>> hashMap = new HashMap<>();
        for (int index = 0; index < offers.size(); index++) {
            ItemStack enchantedBook = offers.get(index).getResult();
            // 获取每个交易结果槽上的附魔书附魔等级
            int level = getBookEnchantment(enchantedBook);
            if (level == -1) {
                continue;
            }
            // 将同一只村民出售的相同附魔相同等级的附魔书分到一组
            ArrayList<Integer> result = hashMap.get(level);
            if (result == null) {
                ArrayList<Integer> list = new ArrayList<>();
                list.add(index + 1);
                hashMap.put(level, list);
            } else {
                // 同一只村民出售了多本相同附魔书，将结果组装起来
                result.add(index + 1);
            }
            this.tradeCount++;
        }
        if (hashMap.isEmpty()) {
            return;
        }
        // 添加结果
        for (Map.Entry<Integer, ArrayList<Integer>> entry : hashMap.entrySet()) {
            this.results.add(new EnchantedBookFindResult(merchant, entry.getValue(), entry.getKey()));
        }
        this.villagerCount++;
    }

    private int getBookEnchantment(ItemStack book) {
        if (book.is(Items.ENCHANTED_BOOK)) {
            return this.predicate.getLevel(book);
        }
        return -1;
    }

    @Override
    protected void notFound() {
        MessageUtils.sendMessage(this.source, KEY.then("cannot_find").translate(this.getTradeName(), FinderCommand.VILLAGER));
    }

    @Override
    protected Component getTradeName() {
        return this.predicate.getDisplayName();
    }

    @Override
    protected LocalizationKey getTradeResultKey() {
        return KEY.then("head");
    }

    public class EnchantedBookFindResult implements Result {
        private final AbstractVillager merchant;
        private final ArrayList<Integer> list;
        private final int level;

        private EnchantedBookFindResult(AbstractVillager merchant, ArrayList<Integer> list, int level) {
            this.merchant = merchant;
            this.list = list;
            this.level = level;
        }

        @Override
        public Component get() {
            Component pos = TextProvider.blockPos(this.villagerPos(), ChatFormatting.GREEN);
            // 村民或流浪商人的名称
            Component villagerName = merchant.getName();
            String indices = getIndexArray(this.list);
            // 获取交易名称
            Component enchantName = predicate.getWithLevel(level);
            return KEY.then("each").translate(pos, villagerName, indices, enchantName);
        }

        @Override
        public BlockPos villagerPos() {
            return this.merchant.blockPosition();
        }

        @Override
        public int compare(Result o1, Result o2) {
            EnchantedBookFindResult result1 = (EnchantedBookFindResult) o1;
            EnchantedBookFindResult result2 = (EnchantedBookFindResult) o2;
            int compare = Integer.compare(result1.level, result2.level);
            if (compare == 0) {
                return MathUtils.compareBlockPos(sourcePos, result1.villagerPos(), result2.villagerPos());
            }
            return -compare;
        }
    }
}
