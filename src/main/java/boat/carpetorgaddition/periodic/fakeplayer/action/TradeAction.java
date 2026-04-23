package boat.carpetorgaddition.periodic.fakeplayer.action;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.command.PlayerActionCommand;
import boat.carpetorgaddition.exception.InfiniteLoopException;
import boat.carpetorgaddition.mixin.accessor.MerchantScreenHandlerAccessor;
import boat.carpetorgaddition.periodic.fakeplayer.FakePlayerUtils;
import boat.carpetorgaddition.util.InventoryUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.util.PlayerUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ChunkPos;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

public class TradeAction extends AbstractPlayerAction {
    /**
     * 交易GUI中左侧按钮的索引
     */
    private final int index;
    /**
     * 是否为虚空交易，虚空交易会在村民所在区块卸载再等待5个游戏刻后进行
     */
    private final boolean voidTrade;
    /**
     * 虚空交易的计时器
     */
    private final MutableInt timer = new MutableInt();
    public static final String INDEX = "index";
    public static final String VOID_TRADE = "void_trade";
    /**
     * 虚空交易等待时间，如果村民被卸载后立即交易，那么交易仍然会被锁定
     */
    public static final int TRADE_WAIT_TIME = 1;
    public static final LocalizationKey KEY = PlayerActionCommand.KEY.then("trade");

    public TradeAction(EntityPlayerMPFake fakePlayer, int index, boolean voidTrade) {
        super(fakePlayer);
        this.index = index;
        this.voidTrade = voidTrade;
        timer.setValue(TRADE_WAIT_TIME);
    }

    @Override
    protected void tick() {
        // 获取按钮的索引
        // 判断当前打开的GUI是否为交易界面
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        if (fakePlayer.containerMenu instanceof MerchantMenu merchantScreenHandler) {
            // 获取计时器，记录村民距离上次被加载的时间是否超过了1游戏刻（区块卸载后村民似乎不会立即卸载）
            if (this.voidTrade) {
                // 获取正在接受交易的村民
                MerchantScreenHandlerAccessor accessor = (MerchantScreenHandlerAccessor) merchantScreenHandler;
                Merchant merchant = accessor.getMerchant();
                if (merchant instanceof AbstractVillager merchantEntity) {
                    // 是否应该等待区块卸载
                    if (shouldWait(merchantEntity)) {
                        this.timer.setValue(TRADE_WAIT_TIME);
                        return;
                    }
                }
                // 检查计时器是否归零
                if (this.timer.intValue() != 0) {
                    // 如果没有归零，计时器递减，然后结束方法
                    this.timer.decrement();
                    return;
                } else {
                    // 如果归零，重置计时器，然后开始交易
                    this.timer.setValue(TRADE_WAIT_TIME);
                }
            }
            // 判断按钮索引是否越界
            if (merchantScreenHandler.getOffers().size() <= this.index) {
                MinecraftServer server = ServerUtils.getServer(fakePlayer);
                MessageUtils.sendMessage(server, KEY.then("error").translate(fakePlayer.getDisplayName()));
                this.stop();
                return;
            }
            // 尝试交易物品
            tryTrade(merchantScreenHandler);
            FakePlayerUtils.mergeEmptyShulkerBox(fakePlayer);
            if (this.voidTrade) {
                // 如果是虚空交易，交易完毕后关闭交易GUI
                fakePlayer.closeContainer();
            }
        }
    }

    // 尝试交易物品
    private void tryTrade(MerchantMenu screenHandler) {
        EntityPlayerMPFake fakePlayer = this.getFakePlayer();
        int loopCount = 0;
        // 如果村民无限交易未启用或当前交易不是虚空交易，则只循环一次
        do {
            loopCount++;
            if (loopCount > 1000) {
                throw new InfiniteLoopException();
            }
            //如果当前交易以锁定，直接结束方法
            MerchantOffer tradeOffer = screenHandler.getOffers().get(this.index);
            if (tradeOffer.isOutOfStock()) {
                return;
            }
            // 选择要交易物品的索引
            screenHandler.setSelectionHint(this.index);
            // 填充交易槽位
            if (switchItem(screenHandler, tradeOffer)) {
                // 判断输出槽是否有物品，如果有，丢出物品，否则停止交易，结束方法
                Slot outputSlot = screenHandler.getSlot(2);
                // 假玩家可能交易出其他交易选项的物品，请参阅：https://bugs.mojang.com/browse/MC-215441
                if (outputSlot.hasItem()) {
                    this.compareAndThrow(screenHandler, 2, tradeOffer.getResult(), fakePlayer);
                    if (CarpetOrgAdditionSettings.VILLAGER_INFINITE_TRADE.value()
                        && CarpetOrgAdditionSettings.FAKE_PLAYER_MAX_ITEM_OPERATION_COUNT.value() > 0
                        && loopCount >= CarpetOrgAdditionSettings.FAKE_PLAYER_MAX_ITEM_OPERATION_COUNT.value()) {
                        return;
                    }
                } else {
                    throw new IllegalStateException("Villager failed to provide the trade item");
                }
            } else {
                // 除非假玩家物品栏内已经没有足够的物品用来交易，否则填充交易槽位不会失败
                return;
            }
            // 如果启用了村民无限交易或当前为虚空交易，则尽可能完成所有交易
        } while (this.voidTrade || CarpetOrgAdditionSettings.VILLAGER_INFINITE_TRADE.value());
    }

    // 选择物品
    private boolean switchItem(MerchantMenu merchantScreenHandler, MerchantOffer tradeOffer) {
        // 获取第一个交易物品
        ItemStack firstBuyItem = tradeOffer.getCostA();// 0索引
        // 获取第二个交易物品
        ItemStack secondBuyItem = tradeOffer.getCostB();// 1索引
        return fillTradeSlot(merchantScreenHandler, firstBuyItem, 0)
               && fillTradeSlot(merchantScreenHandler, secondBuyItem, 1);
    }

    /**
     * 填充交易槽位
     *
     * @param screenHandler 假玩家当前打开的交易GUI
     * @param buyItem       村民的交易物品
     * @param slotIndex     第几个交易物品
     * @return 槽位上的物品是否已经足够参与交易
     */
    private boolean fillTradeSlot(MerchantMenu screenHandler, ItemStack buyItem, int slotIndex) {
        NonNullList<Slot> list = screenHandler.slots;
        // 获取交易槽上的物品
        Slot tradeSlot = screenHandler.getSlot(slotIndex);
        // 如果交易槽上的物品不是需要的物品，就丢弃槽位中的物品
        if (!tradeSlot.getItem().is(buyItem.getItem())) {
            FakePlayerUtils.throwItem(screenHandler, slotIndex, this.getFakePlayer());
        }
        // 如果交易所需的物品为空，或者槽位的物品已经是所需的物品，直接跳过该物品
        if (buyItem.isEmpty() || slotItemCanTrade(tradeSlot.getItem(), buyItem)) {
            return true;
        }
        // 将物品移动到交易槽位
        // 从物品栏寻找物品
        for (int index = 3; index < list.size(); index++) {
            // 获取当前槽位上的物品
            ItemStack itemStack = list.get(index).getItem();
            Predicate<ItemStack> predicate = getStackPredicate(buyItem, tradeSlot.getItem());
            if (predicate.test(itemStack)) {
                // 如果匹配，将当前物品移动到交易槽位
                if (FakePlayerUtils.withKeepPickupAndMoveItemStack(screenHandler, index, slotIndex, this.getFakePlayer())) {
                    // 如果假玩家填充交易槽后光标上有剩余的物品，将剩余的物品放回原槽位
                    if (!screenHandler.getCarried().isEmpty()) {
                        FakePlayerUtils.pickupCursorStack(screenHandler, index, this.getFakePlayer());
                    }
                    if (slotItemCanTrade(tradeSlot.getItem(), buyItem)) {
                        return true;
                    }
                }
            }
        }
        // 从潜影盒寻找物品
        if (CarpetOrgAdditionSettings.FAKE_PLAYER_SHULKER_BOX_ITEM_HANDLING.value()) {
            return this.takeFromShulkerBox(screenHandler, buyItem, slotIndex, tradeSlot, false);
        }
        return false;
    }

    // 从潜影盒拿取物品
    private boolean takeFromShulkerBox(MerchantMenu screenHandler, ItemStack buyItem, int slotIndex, Slot tradeSlot, boolean stacking) {
        NonNullList<Slot> list = screenHandler.slots;
        // 从潜影盒寻找物品
        for (int index = 3; index < list.size(); index++) {
            // 用来交易的物品还差多少个满一组
            ItemStack tradeSlotItem = tradeSlot.getItem();
            int difference = tradeSlotItem.isEmpty() ? buyItem.getMaxStackSize() : (tradeSlotItem.getMaxStackSize() - tradeSlotItem.getCount());
            if (difference <= 0) {
                return true;
            }
            // 获取当前槽位上的物品
            ItemStack itemStack = list.get(index).getItem();
            Predicate<ItemStack> predicate = getStackPredicate(buyItem, tradeSlotItem);
            if (InventoryUtils.isShulkerBoxItem(itemStack) && (stacking || itemStack.getCount() == 1)) {
                // 从潜影盒提取物品
                ItemStack content = InventoryUtils.tryPickItemFromStackedNonEmptyShulkerBox(this.getFakePlayer(), itemStack, predicate, difference);
                if (content.isEmpty()) {
                    continue;
                }
                FakePlayerUtils.dropCursorStack(screenHandler, this.getFakePlayer());
                screenHandler.setCarried(content);
                FakePlayerUtils.pickupCursorStack(screenHandler, slotIndex, this.getFakePlayer());
                if (slotItemCanTrade(tradeSlotItem, buyItem)) {
                    return true;
                }
            }
        }
        return !stacking && takeFromShulkerBox(screenHandler, buyItem, slotIndex, tradeSlot, true);
    }

    private Predicate<ItemStack> getStackPredicate(ItemStack buyItem, final ItemStack slotItem) {
        Predicate<ItemStack> predicate;
        if (slotItem.isEmpty()) {
            // 将当前物品直接与村民需要的交易物品进行比较，不比较NBT
            predicate = stack -> buyItem.is(stack.getItem());
        } else {
            // 交易槽位上有物品，将当前物品与交易槽上的物品比较，同时比较物品NBT
            predicate = stack -> ItemStack.isSameItemSameComponents(slotItem, stack);
        }
        return predicate;
    }

    /**
     * 比较并丢出槽位物品<br>
     * 如果槽位上的物品与预期物品相同，则丢出槽位上的物品
     *
     * @see <a href="https://bugs.mojang.com/browse/MC-157977">MC-157977</a>
     * @see <a href="https://bugs.mojang.com/browse/MC-215441">MC-215441</a>
     */
    public void compareAndThrow(AbstractContainerMenu screenHandler, int slotIndex, ItemStack itemStack, EntityPlayerMPFake player) {
        InventoryUtils.assertEmptyStack(screenHandler.getCarried());
        Slot slot = screenHandler.getSlot(slotIndex);
        while (slot.hasItem() && ItemStack.isSameItemSameComponents(itemStack, slot.getItem()) && slot.mayPickup(player)) {
            screenHandler.clicked(slotIndex, FakePlayerUtils.THROW_Q, ContainerInput.THROW, player);
        }
    }

    // 是否应该等待区块卸载
    private boolean shouldWait(AbstractVillager merchant) {
        // 如果村民所在区块没有被加载，可以交易
        ChunkPos chunkPos = merchant.chunkPosition();
        if (ServerUtils.getWorld(merchant).hasChunk(chunkPos.x(), chunkPos.z())) {
            // 检查村民是否存在于任何一个维度，如果不存在，可以交易
            UUID uuid = merchant.getUUID();
            MinecraftServer server = ServerUtils.getServer(merchant);
            if (server == null) {
                return true;
            }
            for (ServerLevel world : server.getAllLevels()) {
                if (world.getEntity(uuid) == null) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    // 检查槽位上的物品是否可以交易
    private boolean slotItemCanTrade(ItemStack slotItem, ItemStack tradeItem) {
        return (slotItem.getCount() >= tradeItem.getCount()) || slotItem.getCount() >= slotItem.getMaxStackSize();
    }

    @Override
    public void onFakePlayerLogout() {
        this.getFakePlayerNullable().ifPresent(PlayerUtils::closeScreen);
    }

    @Override
    public List<Component> info() {
        ArrayList<Component> list = new ArrayList<>();
        // 获取按钮的索引
        LocalizationKey key = this.getInfoLocalizationKey();
        list.add(key.translate(getFakePlayer().getDisplayName(), index + 1));
        if (getFakePlayer().containerMenu instanceof MerchantMenu merchantScreenHandler) {
            // 获取当前交易内容的对象
            MerchantOffer tradeOffer = merchantScreenHandler.getOffers().get(index);
            // 将交易的物品和价格添加到集合中
            list.add(TextBuilder.combineAll("    ",
                    FakePlayerUtils.getWithCountHoverText(tradeOffer.getCostA()), " ",
                    FakePlayerUtils.getWithCountHoverText(tradeOffer.getCostB()), " -> ",
                    FakePlayerUtils.getWithCountHoverText(tradeOffer.getResult())));
            // 如果当前交易已被锁定，将交易已锁定的消息添加到集合，然后直接结束方法并返回集合
            if (tradeOffer.isOutOfStock()) {
                list.add(key.then("disabled").translate());
                return list;
            }
            // 将“交易状态”文本信息添加到集合中
            list.add(key.then("state").translate());
            list.add(TextBuilder.combineAll("    ",
                    FakePlayerUtils.getWithCountHoverText(merchantScreenHandler.getSlot(0).getItem()), " ",
                    FakePlayerUtils.getWithCountHoverText(merchantScreenHandler.getSlot(1).getItem()), " -> ",
                    FakePlayerUtils.getWithCountHoverText(merchantScreenHandler.getSlot(2).getItem())));
        } else {
            // 将假玩家没有打开交易界面的消息添加到集合中
            list.add(key.then("no_villager").translate(getFakePlayer().getDisplayName()));
        }
        return list;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty(INDEX, this.index);
        json.addProperty(VOID_TRADE, this.voidTrade);
        return json;
    }

    @Override
    public LocalizationKey getLocalizationKey() {
        return KEY;
    }

    @Override
    public ActionSerializeType getActionSerializeType() {
        return ActionSerializeType.TRADE;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TradeAction that = (TradeAction) o;
        return index == that.index && voidTrade == that.voidTrade;
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, voidTrade);
    }
}
