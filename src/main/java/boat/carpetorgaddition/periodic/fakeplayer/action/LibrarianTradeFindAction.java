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

import java.util.*;
import java.util.function.Predicate;

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
        if (this.tryComplete(fakePlayer, villager)) {
            this.stop();
            return true;
        }
        return false;
    }

    protected abstract boolean tryComplete(EntityPlayerMPFake fakePlayer, Villager villager);

    @Nullable
    protected Triple hasTargetTrade(Villager villager, int minLevel, @Nullable Holder<Enchantment> holder) {
        if (minLevel != -1 && minLevel <= 0) {
            throw new IllegalArgumentException("Invalid enchantment level: %s".formatted(minLevel));
        }
        MerchantOffers offers = villager.getOffers();
        for (MerchantOffer offer : offers) {
            ItemStack result = offer.getResult();
            ItemEnchantments enchantments = result.get(DataComponents.STORED_ENCHANTMENTS);
            if (enchantments == null) {
                return null;
            }
            for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                int level = entry.getIntValue();
                Holder<Enchantment> enchantment = entry.getKey();
                if ((holder == null || enchantment.equals(holder))
                    && level >= (minLevel == -1 ? enchantment.value().getMaxLevel() : minLevel)
                    && this.isFairPrice(enchantment, offer.getBaseCostA().getCount())) {
                    return new Triple(enchantment, level, TradePrice.of(level));
                }
            }
        }
        return null;
    }

    protected abstract boolean isFairPrice(Holder<Enchantment> enchantment, int price);

    protected void onComplete(Villager villager, Triple triple) {
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
                        .setColor(PriceLevel.getPriceLevel(triple.price().asInt(), range.getIntKey(), range.getIntValue()).getColor())
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
        this.appendInfoMessage(list, key);
        list.add(key.then("count").translate(this.refreshCount));
        MinecraftServer server = ServerUtils.getServer(fakePlayer);
        list.add(key.then("time").translate(CommonTexts.tickToTime(ServerUtils.getCurrentGameTick(server) - this.startTime)));
        return list;
    }

    protected abstract void appendInfoMessage(List<Component> list, LocalizationKey key);

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        String fieldName = this.getSerializeFieldName();
        JsonObject action = this.getActionJson();
        json.add(fieldName, action);
        json.add("lectern_pos", toJson(this.lecternPos));
        json.addProperty("start_time", this.startTime);
        json.addProperty("refresh_count", this.refreshCount);
        return json;
    }

    protected abstract String getSerializeFieldName();

    protected abstract JsonObject getActionJson();

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
        protected boolean tryComplete(EntityPlayerMPFake fakePlayer, Villager villager) {
            Triple triple = this.hasTargetTrade(villager, this.minLevel, this.enchantment);
            if (triple == null) {
                return false;
            }
            this.onComplete(villager, triple);
            return true;
        }

        @Override
        protected boolean isFairPrice(Holder<Enchantment> enchantment, int price) {
            return price <= this.maxPrice;
        }

        @Override
        protected void appendInfoMessage(List<Component> list, LocalizationKey key) {
            list.add(key.then("enchantment").translate(EnchantmentUtils.getName(this.enchantment)));
            int maxLevel = EnchantmentUtils.getMaxLevel(this.enchantment);
            TextBuilder levelText = key.then(this.minLevel == maxLevel ? "max_level" : "level").builder(this.minLevel);
            levelText.setHover(key.then("level").then("prompt").translate(maxLevel));
            list.add(levelText.build());
            Int2IntMap.Entry range = getPriceRange(this.enchantment, this.minLevel);
            int minPrice = range.getIntKey();
            TextBuilder priceText = key.then(minPrice == this.maxPrice ? "min_price" : "price").builder(this.maxPrice);
            priceText.setHover(key.then("price").then("prompt").translate(range.getIntKey(), range.getIntValue(), this.minLevel));
            list.add(priceText.build());
        }

        @Override
        protected String getSerializeFieldName() {
            return "specific";
        }

        @Override
        protected JsonObject getActionJson() {
            JsonObject json = new JsonObject();
            json.addProperty("enchantment", this.enchantment.key().identifier().toString());
            json.addProperty("min_level", this.minLevel);
            json.addProperty("max_price", this.maxPrice);
            return json;
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
        protected boolean tryComplete(EntityPlayerMPFake fakePlayer, Villager villager) {
            Triple triple = this.hasTargetTrade(villager, -1, null);
            if (triple == null) {
                return false;
            }
            this.onComplete(villager, triple);
            return true;
        }

        @Override
        protected boolean isFairPrice(Holder<Enchantment> enchantment, int price) {
            return price <= PriceLevel.getPriceUpperBound(this.priceLevel, enchantment);
        }

        @Override
        protected void appendInfoMessage(List<Component> list, LocalizationKey key) {
            // TODO
        }

        @Override
        protected String getSerializeFieldName() {
            return "any";
        }

        @Override
        protected JsonObject getActionJson() {
            JsonObject json = new JsonObject();
            json.addProperty("price", this.priceLevel.name().toLowerCase(Locale.ROOT));
            return json;
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

        protected LibrarianMissingTradeFindAction(@Nullable EntityPlayerMPFake fakePlayer, BlockPos lecternPos, BlockPos from, BlockPos to, PriceLevel priceLevel, long startTime) {
            super(fakePlayer, lecternPos, startTime);
            this.from = from;
            this.to = to;
            this.priceLevel = priceLevel;
        }

        @Override
        protected boolean tryComplete(EntityPlayerMPFake fakePlayer, Villager villager) {
            Triple triple = this.hasTargetTrade(villager, -1, null);
            if (triple != null) {
                ServerLevel world = ServerUtils.getWorld(fakePlayer);
                boolean exclusive = ServerUtils.getEntities(world, this.from, this.to, Villager.class)
                        .stream()
                        .filter(value -> value != villager)
                        .allMatch(value -> this.hasTargetTrade(value, -1, triple.enchantment()) == null);
                if (exclusive) {
                    this.onComplete(villager, triple);
                }
            }
            return false;
        }

        @Override
        protected boolean isFairPrice(Holder<Enchantment> enchantment, int price) {
            return price <= PriceLevel.getPriceUpperBound(this.priceLevel, enchantment);
        }

        @Override
        protected void appendInfoMessage(List<Component> list, LocalizationKey key) {
            // TODO
        }

        @Override
        protected String getSerializeFieldName() {
            return "missing";
        }

        @Override
        protected JsonObject getActionJson() {
            JsonObject json = new JsonObject();
            json.add("from", toJson(this.from));
            json.add("to", toJson(this.to));
            json.addProperty("price", this.priceLevel.name().toLowerCase(Locale.ROOT));
            return json;
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

        public static PriceLevel getPriceLevel(int price, int min, int max) {
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

        public static int getPriceUpperBound(PriceLevel level, Holder<Enchantment> enchantment) {
            Int2IntMap.Entry range = getPriceRange(enchantment, enchantment.value().getMaxLevel());
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

    public interface TradePrice {
        boolean isFairPrice(int price);

        int asInt();

        static TradePrice of(int maxPrice) {
            return new TradePrice() {

                @Override
                public boolean isFairPrice(int price) {
                    return price <= maxPrice;
                }

                @Override
                public int asInt() {
                    return maxPrice;
                }
            };
        }

        static TradePrice of(PriceLevel level, Holder.Reference<Enchantment> enchantment) {
            return new TradePrice() {

                @Override
                public boolean isFairPrice(int price) {
                    return price <= this.asInt();
                }

                @Override
                public int asInt() {
                    return PriceLevel.getPriceUpperBound(level, enchantment);
                }
            };
        }
    }

    public record Triple(Holder<Enchantment> enchantment, int level, TradePrice price) implements Predicate<MerchantOffer> {
        @Override
        public boolean test(MerchantOffer offer) {
            return this.getEnchantmentBookLevel(offer) != -1;
        }

        public int getEnchantmentBookLevel(MerchantOffer offer) {
            if (this.price.isFairPrice(offer.getBaseCostA().getCount())) {
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
            }
            return -1;
        }
    }
}
