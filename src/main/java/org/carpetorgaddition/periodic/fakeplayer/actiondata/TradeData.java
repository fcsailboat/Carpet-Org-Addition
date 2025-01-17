package org.carpetorgaddition.periodic.fakeplayer.actiondata;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.text.MutableText;
import net.minecraft.village.TradeOffer;
import org.apache.commons.lang3.mutable.MutableInt;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerTrade;
import org.carpetorgaddition.util.TextUtils;

import java.util.ArrayList;

public class TradeData extends AbstractActionData {
    private static final String INDEX = "index";
    private static final String VOID_TRADE = "void_trade";
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

    public TradeData(int index, boolean voidTrade) {
        this.index = index;
        this.voidTrade = voidTrade;
        timer.setValue(FakePlayerTrade.TRADE_WAIT_TIME);
    }

    public static TradeData load(JsonObject json) {
        int index = json.get(INDEX).getAsInt();
        boolean voidTrade = json.get(VOID_TRADE).getAsBoolean();
        return new TradeData(index, voidTrade);
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty(INDEX, this.index);
        json.addProperty(VOID_TRADE, this.voidTrade);
        return json;
    }

    @Override
    public ArrayList<MutableText> info(EntityPlayerMPFake fakePlayer) {
        ArrayList<MutableText> list = new ArrayList<>();
        // 获取按钮的索引
        list.add(TextUtils.translate("carpet.commands.playerAction.info.trade.item", fakePlayer.getDisplayName(), index + 1));
        if (fakePlayer.currentScreenHandler instanceof MerchantScreenHandler merchantScreenHandler) {
            // 获取当前交易内容的对象
            TradeOffer tradeOffer = merchantScreenHandler.getRecipes().get(index);
            // 将交易的物品和价格添加到集合中
            list.add(TextUtils.appendAll("    ",
                    getWithCountHoverText(tradeOffer.getDisplayedFirstBuyItem()), " ",
                    getWithCountHoverText(tradeOffer.getDisplayedSecondBuyItem()), " -> ",
                    getWithCountHoverText(tradeOffer.getSellItem())));
            // 如果当前交易已被锁定，将交易已锁定的消息添加到集合，然后直接结束方法并返回集合
            if (tradeOffer.isDisabled()) {
                list.add(TextUtils.translate("carpet.commands.playerAction.info.trade.disabled"));
                return list;
            }
            // 将“交易状态”文本信息添加到集合中
            list.add(TextUtils.translate("carpet.commands.playerAction.info.trade.state"));
            list.add(TextUtils.appendAll("    ",
                    getWithCountHoverText(merchantScreenHandler.getSlot(0).getStack()), " ",
                    getWithCountHoverText(merchantScreenHandler.getSlot(1).getStack()), " -> ",
                    getWithCountHoverText(merchantScreenHandler.getSlot(2).getStack())));
        } else {
            // 将假玩家没有打开交易界面的消息添加到集合中
            list.add(TextUtils.translate("carpet.commands.playerAction.info.trade.no_villager", fakePlayer.getDisplayName()));
        }
        return list;
    }

    public int getIndex() {
        return index;
    }

    public boolean isVoidTrade() {
        return voidTrade;
    }

    public MutableInt getTimer() {
        return timer;
    }
}
