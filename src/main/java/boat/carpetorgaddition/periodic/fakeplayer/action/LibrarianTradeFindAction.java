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
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
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
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public abstract class LibrarianTradeFindAction extends AbstractPlayerAction {
    protected final BlockPos lecternPos;
    /**
     * 刷交易的开始时间
     */
    protected final long startTime;
    protected int refreshCount = 0;
    /**
     * 是否正在挖掘方块
     */
    private boolean diggingBlock = false;
    /**
     * 已经缺货的时间
     */
    private long outOfStockTicks = 0L;
    /**
     * 是否已经发送缺货通知
     */
    private boolean outOfStockNoticeSent = false;
    /**
     * 是否已经发送交易锁定通知
     */
    private boolean lockedNoticeSent = false;
    @Nullable
    private Villager prevVillager = null;
    private LibrarianVillagerPoiCache caches;
    private PlayerStorageInventory inventory;
    public static final LocalizationKey KEY = PlayerActionCommand.KEY.then("librarian");

    protected LibrarianTradeFindAction(@Nullable EntityPlayerMPFake fakePlayer, BlockPos lecternPos, long startTime) {
        super(fakePlayer);
        this.lecternPos = lecternPos;
        this.startTime = startTime;
    }

    public static LibrarianTradeFindAction of(@Nullable EntityPlayerMPFake fakePlayer, BlockPos lecternPos, Holder.Reference<Enchantment> enchantment, int level, int price, long startTime) {
        return new LibrarianSpecificTradeFindAction(fakePlayer, lecternPos, enchantment, level, price, startTime);
    }

    public static LibrarianTradeFindAction of(@Nullable EntityPlayerMPFake fakePlayer, BlockPos lecternPos, PriceLevel priceLevel, long startTime) {
        return new LibrarianAnyTradeFindAction(fakePlayer, lecternPos, startTime, priceLevel);
    }

    public static LibrarianTradeFindAction of(@Nullable EntityPlayerMPFake fakePlayer, BlockPos lecternPos, BlockPos from, BlockPos to, PriceLevel priceLevel, long startTime) {
        return new LibrarianMissingTradeFindAction(fakePlayer, lecternPos, from, to, priceLevel, startTime);
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
        BlockState blockState = world.getBlockState(this.lecternPos);
        if (blockState.is(Blocks.LECTERN)) {
            if (this.checkCompletedAndStop(world)) {
                return;
            }
            this.diggingBlock = true;
        } else if (blockState.isAir() || blockState.is(Blocks.WATER)) {
            if (this.inventory.replenish(itemStack -> itemStack.is(Items.LECTERN))) {
                this.outOfStockTicks = 0L;
                this.outOfStockNoticeSent = false;
                BlockHitResult hitResult = new BlockHitResult(Vec3.atBottomCenterOf(this.lecternPos), Direction.DOWN, this.lecternPos, false);
                PlayerUtils.useItemOn(fakePlayer, hitResult);
                this.refreshCount++;
                if (this.prevVillager != null) {
                    ServerUtils.lookAt(fakePlayer, ServerUtils.getEyePos(this.prevVillager));
                }
            } else {
                this.outOfStockTicks++;
                if (this.outOfStockTicks >= 100L && !this.outOfStockNoticeSent) {
                    MinecraftServer server = ServerUtils.getServer(fakePlayer);
                    MessageUtils.sendEmptyMessage(server);
                    MessageUtils.sendMessage(server, KEY.then("pause").translate(fakePlayer.getDisplayName(), this.getDisplayName()));
                    MessageUtils.sendMessage(server, KEY.then("reason").translate(KEY
                            .then("reason")
                            .then("lectern")
                            .builder()
                            .setColor(ChatFormatting.GRAY)
                            .build()));
                    this.outOfStockNoticeSent = true;
                }
            }
        }
    }

    private boolean checkCompletedAndStop(ServerLevel world) {
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
        if (villager.getVillagerXp() != 0 && !this.lockedNoticeSent) {
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
            this.lockedNoticeSent = true;
        }
        ServerUtils.lookAt(fakePlayer, ServerUtils.getEyePos(villager));
        List<TradeMatch> tradeMatches = this.findMatchingTrade(fakePlayer, villager);
        if (tradeMatches.isEmpty()) {
            return false;
        }
        this.onTradeFound(villager, tradeMatches);
        this.stop();
        return true;
    }

    @NonNull
    protected abstract List<TradeMatch> findMatchingTrade(EntityPlayerMPFake fakePlayer, Villager villager);

    @NonNull
    protected List<TradeMatch> findMatchingOffer(Villager villager, int minLevel, @Nullable Holder<Enchantment> holder) {
        if (minLevel <= 0 && minLevel != -1) {
            throw new IllegalArgumentException("Invalid enchantment level: %s".formatted(minLevel));
        }
        MerchantOffers offers = villager.getOffers();
        ArrayList<TradeMatch> results = new ArrayList<>();
        for (MerchantOffer offer : offers) {
            ItemStack result = offer.getResult();
            ItemEnchantments enchantments = result.get(DataComponents.STORED_ENCHANTMENTS);
            if (enchantments == null) {
                continue;
            }
            for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                int level = entry.getIntValue();
                Holder<Enchantment> enchantment = entry.getKey();
                int price = offer.getBaseCostA().getCount();
                if ((holder == null || enchantment.equals(holder))
                    && level >= (minLevel == -1 ? enchantment.value().getMaxLevel() : minLevel)
                    && this.isPriceAcceptable(enchantment, price)) {
                    results.add(new TradeMatch(enchantment, level, price));
                }
            }
        }
        return results;
    }

    protected abstract boolean isPriceAcceptable(Holder<Enchantment> enchantment, int price);

    protected void onTradeFound(Villager villager, List<TradeMatch> tradeMatches) {
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        // 在原版中，拴绳无法拴住村民，将拴绳移出主手是为了与拴绳可拴村民等功能兼容
        this.inventory.replenish(itemStack -> !(itemStack.is(Items.NAME_TAG) || itemStack.is(Items.VILLAGER_SPAWN_EGG) || itemStack.is(Items.LEAD)));
        villager.mobInteract(fakePlayer, InteractionHand.MAIN_HAND);
        boolean trade = this.tryLockTrade(fakePlayer, villager.getOffers());
        LocalizationKey key = this.getLocalizationKey().then("complete");
        MinecraftServer server = ServerUtils.getServer(fakePlayer);
        MessageUtils.sendEmptyMessage(server);
        for (TradeMatch tradeMatch : tradeMatches) {
            Component name = EnchantmentUtils.getName(tradeMatch.enchantment(), tradeMatch.level());
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
            Int2IntMap.Entry range = getPriceBounds(tradeMatch.enchantment(), tradeMatch.level());
            MessageUtils.sendMessage(server, key
                    .then("price")
                    .translate(key
                            .then("price")
                            .then("value")
                            .builder(tradeMatch.price(), range.getIntKey(), range.getIntValue())
                            .setColor(PriceLevel.fromPrice(tradeMatch.price(), range.getIntKey(), range.getIntValue()).getColor())
                            .build()));
            CarpetOrgAddition.LOGGER.info(
                    "{} has now rolled an enchanted book with {}, refresh count: {}, time taken: {} ticks",
                    fakePlayer.getName().getString(),
                    name.getString(),
                    this.refreshCount,
                    tick
            );
        }
        MessageUtils.sendMessage(server, key
                .then(trade ? "locked" : "unlocked")
                .builder()
                .setGrayItalic()
                .build());
        PlayerUtils.closeScreen(fakePlayer);
    }

    private boolean tryLockTrade(EntityPlayerMPFake fakePlayer, MerchantOffers offers) {
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
        this.appendInfo(list, key);
        list.add(key.then("count").translate(this.refreshCount));
        MinecraftServer server = ServerUtils.getServer(fakePlayer);
        list.add(key.then("time").translate(CommonTexts.tickToTime(ServerUtils.getCurrentGameTick(server) - this.startTime)));
        return list;
    }

    protected abstract void appendInfo(List<Component> list, LocalizationKey key);

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", this.getJsonFieldName());
        this.writeActionData(json);
        json.add("lectern_pos", toJson(this.lecternPos));
        json.addProperty("start_time", this.startTime);
        json.addProperty("refresh_count", this.refreshCount);
        return json;
    }

    protected abstract String getJsonFieldName();

    protected abstract void writeActionData(JsonObject json);

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

    /**
     * 获取附魔书交易价格区间
     *
     * @param enchantment 魔咒类型
     * @param level       魔咒等级
     * @see <a href="https://zh.minecraft.wiki/w/%E4%BA%A4%E6%98%93#%E5%9B%BE%E4%B9%A6%E7%AE%A1%E7%90%86%E5%91%98">交易#图书管理员</a>
     */
    public static Int2IntMap.Entry getPriceBounds(Holder<Enchantment> enchantment, int level) {
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

    public static class LibrarianSpecificTradeFindAction extends LibrarianTradeFindAction {
        private final Holder.Reference<Enchantment> enchantment;
        private final int minLevel;
        private final int maxPrice;

        protected LibrarianSpecificTradeFindAction(@Nullable EntityPlayerMPFake fakePlayer, BlockPos lecternPos, Holder.Reference<Enchantment> enchantment, int level, int price, long startTime) {
            super(fakePlayer, lecternPos, startTime);
            this.enchantment = enchantment;
            this.minLevel = level == -1 ? enchantment.value().getMaxLevel() : level;
            this.maxPrice = price == -1 ? Integer.MAX_VALUE : price;
        }

        @Override
        protected @NonNull List<TradeMatch> findMatchingTrade(EntityPlayerMPFake fakePlayer, Villager villager) {
            return this.findMatchingOffer(villager, this.minLevel, this.enchantment);
        }

        @Override
        protected boolean isPriceAcceptable(Holder<Enchantment> enchantment, int price) {
            return price <= this.maxPrice;
        }

        @Override
        protected void appendInfo(List<Component> list, LocalizationKey key) {
            list.add(key.then("enchantment").translate(EnchantmentUtils.getName(this.enchantment)));
            int maxLevel = EnchantmentUtils.getMaxLevel(this.enchantment);
            TextBuilder levelText = key.then(this.minLevel == maxLevel ? "max_level" : "level").builder(this.minLevel);
            levelText.setHover(key.then("level").then("prompt").translate(maxLevel));
            list.add(levelText.build());
            Int2IntMap.Entry range = getPriceBounds(this.enchantment, this.minLevel);
            int minPrice = range.getIntKey();
            TextBuilder priceText = key.then(minPrice == this.maxPrice ? "min_price" : "price").builder(this.maxPrice);
            priceText.setHover(key.then("price").then("prompt").translate(range.getIntKey(), range.getIntValue(), this.minLevel));
            list.add(priceText.build());
        }

        @Override
        protected String getJsonFieldName() {
            return "specific";
        }

        @Override
        protected void writeActionData(JsonObject json) {
            json.addProperty("enchantment", this.enchantment.key().identifier().toString());
            json.addProperty("min_level", this.minLevel);
            json.addProperty("max_price", this.maxPrice);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            LibrarianSpecificTradeFindAction that = (LibrarianSpecificTradeFindAction) o;
            return this.startTime == that.startTime
                   && this.refreshCount == that.refreshCount
                   && minLevel == that.minLevel
                   && maxPrice == that.maxPrice
                   && Objects.equals(enchantment, that.enchantment)
                   && Objects.equals(this.lecternPos, that.lecternPos);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.lecternPos, this.refreshCount, this.startTime, this.enchantment, this.minLevel, this.maxPrice);
        }
    }

    public static class LibrarianAnyTradeFindAction extends LibrarianTradeFindAction {
        private final PriceLevel priceLevel;

        protected LibrarianAnyTradeFindAction(@Nullable EntityPlayerMPFake fakePlayer, BlockPos lecternPos, long startTime, PriceLevel priceLevel) {
            super(fakePlayer, lecternPos, startTime);
            this.priceLevel = priceLevel;
        }

        @Override
        protected @NonNull List<TradeMatch> findMatchingTrade(EntityPlayerMPFake fakePlayer, Villager villager) {
            return this.findMatchingOffer(villager, -1, null);
        }

        @Override
        protected boolean isPriceAcceptable(Holder<Enchantment> enchantment, int price) {
            return price <= PriceLevel.getMaxPrice(this.priceLevel, enchantment);
        }

        @Override
        protected void appendInfo(List<Component> list, LocalizationKey key) {
            list.add(key.then("enchantment").translate(key.then("enchantment").then("any").builder().setItalic().build()));
            list.add(key.then("level").translate(key.then("level").then("max").translate()));
            list.add(key.then("price").then("upper_limit").translate(key.then("price").then(this.priceLevel.name().toLowerCase(Locale.ROOT)).translate()));
        }

        @Override
        protected String getJsonFieldName() {
            return "any";
        }

        @Override
        protected void writeActionData(JsonObject json) {
            json.addProperty("price", this.priceLevel.name().toLowerCase(Locale.ROOT));
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            LibrarianAnyTradeFindAction that = (LibrarianAnyTradeFindAction) o;
            return this.startTime == that.startTime
                   && this.refreshCount == that.refreshCount
                   && priceLevel == that.priceLevel
                   && Objects.equals(this.lecternPos, that.lecternPos);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.lecternPos, this.refreshCount, this.startTime, this.priceLevel);
        }
    }

    public static class LibrarianMissingTradeFindAction extends LibrarianTradeFindAction {
        private final BlockPos from;
        private final BlockPos to;
        private final PriceLevel priceLevel;
        private boolean noMissingNoticeSent = false;
        private long callsUntilNoMissingRecheck = 60L;

        protected LibrarianMissingTradeFindAction(@Nullable EntityPlayerMPFake fakePlayer, BlockPos lecternPos, BlockPos from, BlockPos to, PriceLevel priceLevel, long startTime) {
            super(fakePlayer, lecternPos, startTime);
            this.from = from;
            this.to = to;
            this.priceLevel = priceLevel;
        }

        @Override
        protected @NonNull List<TradeMatch> findMatchingTrade(EntityPlayerMPFake fakePlayer, Villager villager) {
            this.callsUntilNoMissingRecheck--;
            if (this.callsUntilNoMissingRecheck == 0L) {
                if (!this.noMissingNoticeSent) {
                    MinecraftServer server = ServerUtils.getServer(fakePlayer);
                    if (this.getMissingEnchantments(ServerUtils.getWorld(fakePlayer), server).isEmpty()) {
                        Component head = KEY.then("unfeasible").translate(fakePlayer.getDisplayName(), this.getDisplayName());
                        MessageUtils.sendEmptyMessage(server);
                        MessageUtils.sendMessage(server, head);
                        LocalizationKey reason = KEY.then("reason");
                        MessageUtils.sendMessage(server, reason
                                .translate(reason
                                        .then("no_missing")
                                        .builder()
                                        .setColor(ChatFormatting.GRAY)
                                        .build()));
                        this.noMissingNoticeSent = true;
                    } else {
                        this.callsUntilNoMissingRecheck = 60L;
                    }
                }
            }
            List<TradeMatch> tradeMatches = this.findMatchingOffer(villager, -1, null);
            if (!tradeMatches.isEmpty()) {
                ServerLevel world = ServerUtils.getWorld(fakePlayer);
                boolean exclusive = ServerUtils.getEntities(world, this.from, this.to, Villager.class)
                        .stream()
                        .filter(value -> value != villager)
                        .allMatch(value -> tradeMatches.stream().allMatch(tradeMatch -> this.findMatchingOffer(value, -1, tradeMatch.enchantment()).isEmpty()));
                if (exclusive) {
                    return tradeMatches;
                }
            }
            return List.of();
        }

        @Override
        protected boolean isPriceAcceptable(Holder<Enchantment> enchantment, int price) {
            return price <= PriceLevel.getMaxPrice(this.priceLevel, enchantment);
        }

        @Override
        protected void appendInfo(List<Component> list, LocalizationKey key) {
            EntityPlayerMPFake fakePlayer = this.getFakePlayer();
            MinecraftServer server = ServerUtils.getServer(fakePlayer);
            ServerLevel world = ServerUtils.getWorld(fakePlayer);
            TextBuilder builder = key.then("enchantment").then("missing").builder(CommonTexts.blockPos(this.from), CommonTexts.blockPos(this.to));
            List<Holder<Enchantment>> missing = getMissingEnchantments(world, server);
            if (!missing.isEmpty()) {
                TextJoiner joiner = new TextJoiner();
                missing.forEach(holder -> joiner.newline(EnchantmentUtils.getName(holder)));
                builder.setHover(joiner.join());
            }
            list.add(key.then("enchantment").translate(builder.build()));
            list.add(key.then("level").translate(key.then("level").then("max").translate()));
            list.add(key.then("price").then("upper_limit").translate(key.then("price").then(this.priceLevel.name().toLowerCase(Locale.ROOT)).translate()));
        }

        private List<Holder<Enchantment>> getMissingEnchantments(ServerLevel world, MinecraftServer server) {
            Set<Holder<Enchantment>> existing = ServerUtils.getEntities(world, this.from, this.to, Villager.class)
                    .stream()
                    .map(AbstractVillager::getOffers)
                    .flatMap(Collection::stream)
                    .map(offer -> {
                        ItemStack result = offer.getResult();
                        ItemEnchantments enchantments = result.get(DataComponents.STORED_ENCHANTMENTS);
                        if (enchantments == null) {
                            return null;
                        }
                        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                            int level = entry.getIntValue();
                            Holder<Enchantment> enchantment = entry.getKey();
                            int price = offer.getBaseCostA().getCount();
                            if (level >= (enchantment.value().getMaxLevel()) && this.isPriceAcceptable(enchantment, price)) {
                                return enchantment;
                            }
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            return server.registryAccess()
                    .lookup(Registries.ENCHANTMENT)
                    .map(Registry::asHolderIdMap)
                    .stream()
                    .flatMap(holders -> StreamSupport.stream(holders.spliterator(), false))
                    .filter(holder -> holder.is(EnchantmentTags.TRADEABLE))
                    .filter(holder -> !existing.contains(holder))
                    .toList();
        }

        @Override
        protected String getJsonFieldName() {
            return "missing";
        }

        @Override
        protected void writeActionData(JsonObject json) {
            json.add("from", toJson(this.from));
            json.add("to", toJson(this.to));
            json.addProperty("price", this.priceLevel.name().toLowerCase(Locale.ROOT));
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            LibrarianMissingTradeFindAction that = (LibrarianMissingTradeFindAction) o;
            return this.startTime == that.startTime
                   && this.refreshCount == that.refreshCount
                   && Objects.equals(from, that.from)
                   && Objects.equals(to, that.to)
                   && priceLevel == that.priceLevel
                   && Objects.equals(this.lecternPos, that.lecternPos);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.lecternPos, this.refreshCount, this.startTime, this.from, this.to, this.priceLevel);
        }
    }

    public enum PriceLevel {
        MIN,
        LOW,
        MEDIUM,
        HIGH;

        public static PriceLevel fromPrice(int price, int min, int max) {
            if (price == min) {
                return PriceLevel.MIN;
            }
            int total = max - min + 1;
            if (price < min + total / 3) {
                return PriceLevel.LOW;
            } else if (price < min + (2 * total) / 3) {
                return PriceLevel.MEDIUM;
            } else {
                return PriceLevel.HIGH;
            }
        }

        public static int getMaxPrice(PriceLevel level, Holder<Enchantment> enchantment) {
            Int2IntMap.Entry range = getPriceBounds(enchantment, enchantment.value().getMaxLevel());
            int min = range.getIntKey();
            int max = range.getIntValue();
            int total = max - min + 1;
            int value = switch (level) {
                case MIN -> min;
                case LOW -> min + total / 3 - 1;
                case MEDIUM -> min + (2 * total) / 3 - 1;
                case HIGH -> max;
            };
            return Math.clamp(value, 1, 64);
        }

        public ChatFormatting getColor() {
            return switch (this) {
                case MIN, LOW -> ChatFormatting.GREEN;
                case MEDIUM -> ChatFormatting.YELLOW;
                case HIGH -> ChatFormatting.DARK_RED;
            };
        }
    }

    public record TradeMatch(Holder<Enchantment> enchantment, int level, int price) {
    }
}
