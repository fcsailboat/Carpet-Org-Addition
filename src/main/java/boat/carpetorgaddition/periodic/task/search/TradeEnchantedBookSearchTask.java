package boat.carpetorgaddition.periodic.task.search;

import boat.carpetorgaddition.command.FinderCommand;
import boat.carpetorgaddition.util.EnchantmentUtils;
import boat.carpetorgaddition.util.IdentityUtils;
import boat.carpetorgaddition.util.MathUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.wheel.common.CommonTexts;
import boat.carpetorgaddition.wheel.predicate.EnchantedBookPredicate;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TradeEnchantedBookSearchTask extends AbstractTradeSearchTask {
    private final MinecraftServer server;
    private final EnchantedBookPredicate predicate;
    private static final LocalizationKey KEY = TRADE.then("enchanted_book");

    public TradeEnchantedBookSearchTask(Level world, AABB box, BlockPos sourcePos, CommandSourceStack source, EnchantedBookPredicate predicate, ServerPlayer player) {
        super(world, box, sourcePos, source, player);
        this.server = source.getServer();
        this.predicate = predicate;
    }

    @Override
    protected void searchVillager(AbstractVillager villager) {
        MerchantOffers offers = villager.getOffers();
        // 键：附魔书的等级，值：同一只村民出售的相同附魔书的索引集合
        Map<Holder<Enchantment>, Int2ObjectMap<IntList>> map = new HashMap<>();
        for (int index = 0; index < offers.size(); index++) {
            ItemStack book = offers.get(index).getResult();
            // 获取每个交易结果槽上的附魔书附魔等级
            List<Object2IntMap.Entry<Holder<Enchantment>>> entries = this.getBookEnchantments(book);
            if (entries.isEmpty()) {
                continue;
            }
            for (Object2IntMap.Entry<Holder<Enchantment>> entry : entries) {
                int level = entry.getIntValue();
                // 将同一只村民出售的相同附魔相同等级的附魔书分到一组
                Holder<Enchantment> enchantment = entry.getKey();
                Int2ObjectMap<IntList> levelToIndices = map.computeIfAbsent(enchantment, _ -> new Int2ObjectArrayMap<>());
                // 同一只村民出售了多本相同附魔书，将结果组装起来
                IntList indices = levelToIndices.computeIfAbsent(level, _ -> new IntArrayList());
                indices.add(index + 1);
            }
            this.tradeCount++;
        }
        if (map.isEmpty()) {
            return;
        }
        // 添加结果
        for (Map.Entry<Holder<Enchantment>, Int2ObjectMap<IntList>> entry : map.entrySet()) {
            Holder<Enchantment> enchantment = entry.getKey();
            for (Int2ObjectMap.Entry<IntList> indicesAndLevel : entry.getValue().int2ObjectEntrySet()) {
                this.results.add(new EnchantedBookFindResult(villager, indicesAndLevel.getValue(), enchantment, indicesAndLevel.getIntKey()));
            }
        }
        this.villagerCount++;
    }

    private List<Object2IntMap.Entry<Holder<Enchantment>>> getBookEnchantments(ItemStack book) {
        if (book.is(Items.ENCHANTED_BOOK)) {
            ItemEnchantments enchantments = book.get(DataComponents.STORED_ENCHANTMENTS);
            if (enchantments == null) {
                return List.of();
            }
            ArrayList<Object2IntMap.Entry<Holder<Enchantment>>> list = new ArrayList<>();
            for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                if (this.predicate.test(entry.getKey())) {
                    list.add(entry);
                }
            }
            return list;
        }
        return List.of();
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
        private final AbstractVillager villager;
        private final IntList indexList;
        private final Holder<Enchantment> enchantment;
        private final int level;

        private EnchantedBookFindResult(AbstractVillager villager, IntList indexList, Holder<Enchantment> enchantment, int level) {
            this.villager = villager;
            this.indexList = indexList;
            this.enchantment = enchantment;
            this.level = level;
        }

        @Override
        public Component get() {
            Component pos = CommonTexts.blockPos(this.villagerPos(), ChatFormatting.GREEN);
            // 村民或流浪商人的名称
            Component villagerName = villager.getName();
            String indices = getIndexArray(this.indexList);
            // 获取交易名称
            TextBuilder builder = TextBuilder.of(EnchantmentUtils.getName(this.enchantment, this.level));
            builder.setHover(IdentityUtils.getIdAsString(server.registryAccess(), this.enchantment));
            return KEY.then("each").translate(pos, villagerName, indices, builder.build());
        }

        @Override
        public BlockPos villagerPos() {
            return this.villager.blockPosition();
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
