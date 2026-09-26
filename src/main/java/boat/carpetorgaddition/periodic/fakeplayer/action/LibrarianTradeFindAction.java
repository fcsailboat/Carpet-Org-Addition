package boat.carpetorgaddition.periodic.fakeplayer.action;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.command.PlayerActionCommand;
import boat.carpetorgaddition.periodic.PlayerComponentCoordinator;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.periodic.fakeplayer.BlockExcavator;
import boat.carpetorgaddition.util.EnchantmentUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.PlayerUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.ItemIdentity;
import boat.carpetorgaddition.wheel.MenuController;
import boat.carpetorgaddition.wheel.common.CommonTexts;
import boat.carpetorgaddition.wheel.inventory.PlayerStorageInventory;
import boat.carpetorgaddition.wheel.misc.LibrarianTradeTriples;
import boat.carpetorgaddition.wheel.misc.LibrarianVillagerPoiCache;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import boat.carpetorgaddition.wheel.text.TextJoiner;
import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public class LibrarianTradeFindAction extends AbstractPlayerAction {
    private final BlockPos lecternPos;
    private final LibrarianTradeTriples triples;
    /**
     * 刷交易的开始时间
     */
    private final long startTime;
    private int refreshCount = 0;
    /**
     * 是否正在挖掘方块
     */
    private boolean diggingBlock = false;
    /**
     * 已经缺货的时间
     */
    private long outOfStockTime = 0L;
    /**
     * 是否已经发送缺货通知
     */
    private boolean outOfStockNotice = false;
    /**
     * 是否已经发送交易锁定通知
     */
    private boolean lockedNotice = false;
    @Nullable
    private Villager prevVillager = null;
    private LibrarianVillagerPoiCache caches;
    private PlayerStorageInventory inventory;
    public static final LocalizationKey KEY = PlayerActionCommand.KEY.then("librarian");

    public LibrarianTradeFindAction(@Nullable EntityPlayerMPFake fakePlayer, BlockPos lecternPos, Holder.Reference<Enchantment> enchantmentHolder, int level, int price, long startTime) {
        int minLevel = level == -1 ? enchantmentHolder.value().getMaxLevel() : level;
        int maxPrice = price == -1 ? Integer.MAX_VALUE : price;
        LibrarianTradeTriples triples = LibrarianTradeTriples.of(new Triple(enchantmentHolder, minLevel, maxPrice));
        this(fakePlayer, lecternPos, triples, startTime);
    }

    public LibrarianTradeFindAction(@Nullable EntityPlayerMPFake fakePlayer, BlockPos lecternPos, LibrarianTradeTriples triples, long startTime) {
        super(fakePlayer);
        this.lecternPos = lecternPos;
        this.startTime = startTime;
        this.triples = triples;
    }

    @Override
    protected void tick() {
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        ServerLevel world = ServerUtils.getWorld(fakePlayer);
        if (this.diggingBlock) {
            BlockExcavator blockExcavator = PlayerComponentCoordinator.of(fakePlayer).getBlockExcavator();
            this.inventory.switchToAppropriateTool(world, this.lecternPos);
            ServerUtils.lookAt(fakePlayer, Vec3.atBottomCenterOf(this.lecternPos));
            if (blockExcavator.mining(this.lecternPos, Direction.DOWN)) {
                this.diggingBlock = false;
            }
            return;
        }
        if (this.triples.isEmpty()) {
            return;
        }
        BlockState blockState = world.getBlockState(this.lecternPos);
        if (blockState.is(Blocks.LECTERN)) {
            if (this.checkAndStopIfCompleted(world)) {
                return;
            }
            this.diggingBlock = true;
        } else if (blockState.isAir() || blockState.is(Blocks.WATER)) {
            if (this.inventory.replenish(itemStack -> itemStack.is(Items.LECTERN))) {
                this.outOfStockTime = 0L;
                this.outOfStockNotice = false;
                BlockHitResult hitResult = new BlockHitResult(Vec3.atBottomCenterOf(this.lecternPos), Direction.DOWN, this.lecternPos, false);
                PlayerUtils.useItemOn(fakePlayer, hitResult);
                this.refreshCount++;
                if (this.prevVillager != null) {
                    ServerUtils.lookAt(fakePlayer, ServerUtils.getEyePos(this.prevVillager));
                }
            } else {
                this.outOfStockTime++;
                if (this.outOfStockTime >= 100L && !this.outOfStockNotice) {
                    MinecraftServer server = ServerUtils.getServer(fakePlayer);
                    MessageUtils.sendEmptyMessage(server);
                    MessageUtils.sendMessage(server, KEY.then("pause").translate(fakePlayer.getDisplayName(), this.getDisplayName()));
                    MessageUtils.sendMessage(server, KEY.then("reason").translate(KEY
                            .then("reason")
                            .then("lectern")
                            .builder()
                            .setColor(ChatFormatting.GRAY)
                            .build()));
                    this.outOfStockNotice = true;
                }
            }
        }
    }

    private boolean checkAndStopIfCompleted(ServerLevel world) {
        if (world.getPoiManager().getType(this.lecternPos).filter(type -> type.is(PoiTypes.LIBRARIAN)).isEmpty()) {
            return false;
        }
        Optional<Villager> optional = this.caches.getVillager(world, this.lecternPos);
        if (optional.isEmpty()) {
            return true;
        }
        Villager villager = optional.get();
        this.prevVillager = villager;
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        MinecraftServer server = ServerUtils.getServer(fakePlayer);
        if (villager.getVillagerXp() != 0 && !this.lockedNotice) {
            Component head = KEY.then("unfeasible").translate(fakePlayer.getDisplayName(), this.getDisplayName());
            MessageUtils.sendEmptyMessage(server);
            MessageUtils.sendMessage(server, head);
            LocalizationKey reason = KEY.then("reason");
            MessageUtils.sendMessage(server, reason
                    .translate(reason
                            .then("locked")
                            .builder()
                            .setColor(ChatFormatting.GRAY)
                            .build()));
            this.lockedNotice = true;
        }
        ServerUtils.lookAt(fakePlayer, ServerUtils.getEyePos(villager));
        MerchantOffers offers = villager.getOffers();
        ArrayList<Triple> list = new ArrayList<>();
        for (MerchantOffer offer : offers) {
            List<Triple> triples = this.triples.testAndRemove(offer);
            list.addAll(triples);
        }
        if (list.isEmpty()) {
            return false;
        }
        for (Triple triple : list) {
            this.onComplete(villager, triple);
        }
        this.stop();
        return true;
    }

    private void onComplete(Villager villager, Triple triple) {
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        // 在原版中，拴绳无法拴住村民，将拴绳移出主手是为了与拴绳可拴村民等功能兼容
        this.inventory.replenish(itemStack -> !(itemStack.is(Items.NAME_TAG) || itemStack.is(Items.VILLAGER_SPAWN_EGG) || itemStack.is(Items.LEAD)));
        villager.mobInteract(fakePlayer, InteractionHand.MAIN_HAND);
        boolean trade = this.tryTrade(fakePlayer, villager.getOffers());
        LocalizationKey key = this.getLocalizationKey().then("complete");
        MinecraftServer server = ServerUtils.getServer(fakePlayer);
        MessageUtils.sendEmptyMessage(server);
        Component name = EnchantmentUtils.getName(triple.enchantment(), triple.level());
        long tick = ServerUtils.getCurrentGameTick(server) - this.startTime;
        MessageUtils.sendMessage(server, key
                .builder(fakePlayer.getDisplayName(), name)
                .setHover(new TextJoiner()
                        .newline(key
                                .then("time_taken")
                                .translate(CommonTexts.tickToTime(tick)))
                        .newline(key
                                .then("refresh_count")
                                .translate(this.refreshCount))
                        .join())
                .build());
        Int2IntMap.Entry range = getPriceRange(triple.enchantment(), triple.level());
        MessageUtils.sendMessage(server, key
                .then("price")
                .translate(key
                        .then("price")
                        .then("value")
                        .builder(triple.price(), range.getIntKey(), range.getIntValue())
                        .setColor(PriceLevel.getPriceLevel(triple.price(), range.getIntKey(), range.getIntValue()).getColor())
                        .build()));
        MessageUtils.sendMessage(server, key
                .then(trade ? "locked" : "unlocked")
                .builder()
                .setGrayItalic()
                .build());
        CarpetOrgAddition.LOGGER.info(
                "{} has now rolled an enchanted book with {}, refresh count: {}, time taken: {} ticks",
                fakePlayer.getName().getString(),
                name.getString(),
                this.refreshCount,
                tick
        );
        PlayerUtils.closeScreen(fakePlayer);
    }

    private boolean tryTrade(EntityPlayerMPFake fakePlayer, MerchantOffers offers) {
        if (PlayerUtils.getCurrentScreen(fakePlayer) instanceof MerchantMenu menu) {
            MenuController<MerchantMenu> controller = new MenuController<>(menu, fakePlayer);
            for (int i = 0; i < offers.size(); i++) {
                MerchantOffer offer = offers.get(i);
                ItemStack costA = offer.getCostA();
                ItemStack costB = offer.getCostB();
                if (
                        this.inventory.hasMaterial(new ItemIdentity(costA), costA.getCount(), false)
                        && this.inventory.hasMaterial(new ItemIdentity(costB), costB.getCount(), false)
                ) {
                    TradeAction action = new TradeAction(fakePlayer, i, false);
                    // 交易一次以锁定交易
                    return action.tradeOnce(controller);
                }
            }
        }
        return false;
    }

    @Override
    public List<Component> info() {
        ArrayList<Component> list = new ArrayList<>();
        LocalizationKey key = this.getInfoLocalizationKey();
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        list.add(key.translate(fakePlayer.getDisplayName()));
        List<Triple> triples = this.triples.getTriples();
        for (int i = 0; i < triples.size(); i++) {
            Triple triple = triples.get(i);
            list.add(key.then("enchantment").translate(EnchantmentUtils.getName(triple.enchantment())));
            int maxLevel = EnchantmentUtils.getMaxLevel(triple.enchantment());
            TextBuilder levelText = key.then(triple.level() == maxLevel ? "max_level" : "level").builder(triple.level());
            levelText.setHover(key.then("level").then("prompt").translate(maxLevel));
            list.add(levelText.build());
            Int2IntMap.Entry range = getPriceRange(triple.enchantment(), triple.level());
            int minPrice = range.getIntKey();
            TextBuilder priceText = key.then(minPrice == triple.price() ? "min_price" : "price").builder(triple.price());
            priceText.setHover(key.then("price").then("prompt").translate(range.getIntKey(), range.getIntValue(), triple.level()));
            list.add(priceText.build());
            if (i < triples.size() - 1) {
                list.add(TextBuilder.create("-".repeat(30)));
            }
        }
        list.add(key.then("count").translate(this.refreshCount));
        MinecraftServer server = ServerUtils.getServer(fakePlayer);
        list.add(key.then("time").translate(CommonTexts.tickToTime(ServerUtils.getCurrentGameTick(server) - this.startTime)));
        return list;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        List<Triple> triples = this.triples.getTriples();
        // TODO
        Triple triple = triples.getFirst();
        json.addProperty("enchantment", triple.enchantment().key().identifier().toString());
        json.add("block_pos", toJson(this.lecternPos));
        json.addProperty("min_level", triple.level());
        json.addProperty("max_price", triple.price());
        json.addProperty("start_time", this.startTime);
        json.addProperty("refresh_count", this.refreshCount);
        return json;
    }

    @Override
    protected LocalizationKey getLocalizationKey() {
        return KEY;
    }

    @Override
    public ActionSerializeType getActionSerializeType() {
        return ActionSerializeType.LIBRARIAN;
    }

    @Override
    protected void onAssignPlayer() {
        this.caches = ServerComponentCoordinator.of(ServerUtils.getServer(this.getFakePlayer())).getLibrarianVillagerPoiCache();
        this.inventory = PlayerStorageInventory.of(this.getFakePlayer());
    }

    @Override
    protected void onClearPlayer() {
        this.caches = null;
        this.inventory = null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LibrarianTradeFindAction action = (LibrarianTradeFindAction) o;
        return Objects.equals(this.lecternPos, action.lecternPos) && Objects.equals(this.triples, action.triples);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.lecternPos, this.triples);
    }

    /**
     * 获取附魔书交易价格区间
     *
     * @param enchantment 魔咒类型
     * @param level       魔咒等级
     * @see <a href="https://zh.minecraft.wiki/w/%E4%BA%A4%E6%98%93#%E5%9B%BE%E4%B9%A6%E7%AE%A1%E7%90%86%E5%91%98">交易#图书管理员</a>
     */
    public static Int2IntMap.Entry getPriceRange(Holder<Enchantment> enchantment, int level) {
        int min = 2 + level * 3;
        int max = 6 + level * 13;
        if (enchantment.is(EnchantmentTags.DOUBLE_TRADE_PRICE)) {
            return Int2IntMap.entry(min * 2, max * 2);
        } else {
            return Int2IntMap.entry(min, max);
        }
    }

    public void setRefreshCount(int refreshCount) {
        this.refreshCount = refreshCount;
    }

    public enum PriceLevel {
        LOW,
        MEDIUM,
        HIGH;

        public static PriceLevel getPriceLevel(int price, int min, int max) {
            int total = max - min + 1;
            if (price < min + total / 3) {
                return PriceLevel.LOW;
            } else if (price < min + (2 * total) / 3) {
                return PriceLevel.MEDIUM;
            } else {
                return PriceLevel.HIGH;
            }
        }

        public ChatFormatting getColor() {
            return switch (this) {
                case LOW -> ChatFormatting.GREEN;
                case MEDIUM -> ChatFormatting.YELLOW;
                case HIGH -> ChatFormatting.DARK_RED;
            };
        }
    }

    public record Triple(Holder.Reference<Enchantment> enchantment, int level, int price) implements Predicate<MerchantOffer> {
        @Override
        public boolean test(MerchantOffer offer) {
            return this.getEnchantmentBookLevel(offer) != -1;
        }

        public int getEnchantmentBookLevel(MerchantOffer offer) {
            if (offer.getBaseCostA().getCount() > this.price) {
                return -1;
            }
            ItemEnchantments enchantments = offer.getResult().get(DataComponents.STORED_ENCHANTMENTS);
            if (enchantments == null) {
                return -1;
            }
            for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                if (entry.getKey().equals(this.enchantment)) {
                    int level = entry.getIntValue();
                    return level >= this.level ? level : -1;
                }
            }
            return -1;
        }
    }
}
