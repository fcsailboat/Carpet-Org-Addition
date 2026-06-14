package boat.carpetorgaddition.periodic.fakeplayer.action;

import boat.carpetorgaddition.command.PlayerActionCommand;
import boat.carpetorgaddition.periodic.PlayerComponentCoordinator;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.periodic.fakeplayer.BlockExcavator;
import boat.carpetorgaddition.util.EnchantmentUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.PlayerUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.ItemIdentity;
import boat.carpetorgaddition.wheel.inventory.PlayerStorageInventory;
import boat.carpetorgaddition.wheel.misc.LibrarianVillagerPoiCache;
import boat.carpetorgaddition.wheel.provider.TextProvider;
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

public class LibrarianTradeFindAction extends AbstractPlayerAction {
    private final BlockPos lecternPos;
    private final Holder.Reference<Enchantment> enchantmentHolder;
    /**
     * 刷交易的开始时间
     */
    private final long startTime;
    private int refreshCount = 0;
    /**
     * 最小接受附魔书等级
     */
    private final int minLevel;
    /**
     * 最大接受附魔书价格
     */
    private final int maxPrice;
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
        super(fakePlayer);
        this.lecternPos = lecternPos;
        this.enchantmentHolder = enchantmentHolder;
        this.startTime = startTime;
        this.minLevel = level == -1 ? enchantmentHolder.value().getMaxLevel() : level;
        this.maxPrice = price == -1 ? Integer.MAX_VALUE : price;
    }

    @Override
    protected void tick() {
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        ServerLevel world = ServerUtils.getWorld(fakePlayer);
        if (this.diggingBlock) {
            BlockExcavator blockExcavator = PlayerComponentCoordinator.getCoordinator(fakePlayer).getBlockExcavator();
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
        MerchantOffers offers = villager.getOffers();
        for (MerchantOffer offer : offers) {
            ItemStack itemStack = offer.getBaseCostA();
            int count = itemStack.getCount();
            int level = this.verify(offer.getResult());
            if (count <= this.maxPrice && level != -1) {
                this.complete(villager, level, itemStack.getCount());
                return true;
            }
        }
        return false;
    }

    private void complete(Villager villager, int level, int price) {
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        // 在原版中，拴绳无法拴住村民，将拴绳移出主手是为了与拴绳可拴村民等功能兼容
        this.inventory.replenish(itemStack -> !(itemStack.is(Items.NAME_TAG) || itemStack.is(Items.VILLAGER_SPAWN_EGG) || itemStack.is(Items.LEAD)));
        villager.mobInteract(fakePlayer, InteractionHand.MAIN_HAND);
        boolean trade = this.tryTrade(fakePlayer, villager.getOffers());
        LocalizationKey key = this.getLocalizationKey().then("complete");
        MinecraftServer server = ServerUtils.getServer(fakePlayer);
        MessageUtils.sendEmptyMessage(server);
        MessageUtils.sendMessage(server, key
                .builder(fakePlayer.getDisplayName(), EnchantmentUtils.getName(this.enchantmentHolder, level))
                .setHover(new TextJoiner()
                        .newline(key
                                .then("time_taken")
                                .translate(TextProvider.tickToTime(ServerUtils.getTime(server) - this.startTime)))
                        .newline(key
                                .then("refresh_count")
                                .translate(this.refreshCount))
                        .join())
                .build());
        Int2IntMap.Entry range = getPriceRange(this.enchantmentHolder, level);
        MessageUtils.sendMessage(server, key
                .then("price")
                .translate(key
                        .then("price")
                        .then("value")
                        .builder(price, range.getIntKey(), range.getIntValue())
                        .setColor(PriceLevel.getPriceLevel(price, range.getIntKey(), range.getIntValue()).getColor())
                        .build()));
        MessageUtils.sendMessage(server, key
                .then(trade ? "locked" : "unlocked")
                .builder()
                .setGrayItalic()
                .build());
        PlayerUtils.closeScreen(fakePlayer);
        this.stop();
    }

    private boolean tryTrade(EntityPlayerMPFake fakePlayer, MerchantOffers offers) {
        if (PlayerUtils.getCurrentScreen(fakePlayer) instanceof MerchantMenu menu) {
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
                    return action.tradeOnce(menu, fakePlayer);
                }
            }
        }
        return false;
    }

    private int verify(ItemStack enchantmentBook) {
        ItemEnchantments enchantments = enchantmentBook.get(DataComponents.STORED_ENCHANTMENTS);
        if (enchantments == null) {
            return -1;
        }
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            if (entry.getKey().equals(this.enchantmentHolder)) {
                int level = entry.getIntValue();
                return level >= this.minLevel ? level : -1;
            }
        }
        return -1;
    }

    @Override
    public List<Component> info() {
        ArrayList<Component> list = new ArrayList<>();
        LocalizationKey key = this.getInfoLocalizationKey();
        list.add(key.translate(this.getFakePlayer().getDisplayName()));
        list.add(key.then("enchantment").translate(EnchantmentUtils.getName(this.enchantmentHolder)));
        int maxLevel = EnchantmentUtils.getMaxLevel(this.enchantmentHolder);
        TextBuilder levelText = key.then(this.minLevel == maxLevel ? "max_level" : "level").builder(this.minLevel);
        levelText.setHover(key.then("level").then("prompt").translate(maxLevel));
        list.add(levelText.build());
        Int2IntMap.Entry range = getPriceRange(this.enchantmentHolder, this.minLevel);
        int minPrice = range.getIntKey();
        TextBuilder priceText = key.then(minPrice == this.maxPrice ? "min_price" : "price").builder(this.maxPrice);
        priceText.setHover(key.then("price").then("prompt").translate(range.getIntKey(), range.getIntValue(), this.minLevel));
        list.add(priceText.build());
        return list;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("enchantment", this.enchantmentHolder.key().identifier().toString());
        json.add("block_pos", toJson(this.lecternPos));
        json.addProperty("min_level", this.minLevel);
        json.addProperty("max_price", this.maxPrice);
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
        this.caches = ServerComponentCoordinator.getCoordinator(ServerUtils.getServer(this.getFakePlayer())).getLibrarianVillagerPoiCache();
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
        return this.minLevel == action.minLevel
               && this.maxPrice == action.maxPrice
               && Objects.equals(this.lecternPos, action.lecternPos)
               && Objects.equals(this.enchantmentHolder, action.enchantmentHolder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.lecternPos, this.enchantmentHolder, this.minLevel, this.maxPrice);
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
}
