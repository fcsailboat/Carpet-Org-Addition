package org.carpetorgaddition.periodic.fakeplayer.actiondata;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.matcher.ItemMatcher;
import org.carpetorgaddition.util.matcher.ItemTagMatcher;
import org.carpetorgaddition.util.matcher.Matcher;

import java.util.ArrayList;

public class InventoryCraftData extends AbstractActionData {
    /**
     * 物品合成所使用的物品栏
     */
    private final Matcher[] matchers = new Matcher[4];

    public InventoryCraftData(Matcher[] matchers) {
        System.arraycopy(matchers, 0, this.matchers, 0, this.matchers.length);
    }

    public static InventoryCraftData load(JsonObject json) {
        Matcher[] matchers = new Matcher[4];
        for (int i = 0; i < matchers.length; i++) {
            String item = json.get(String.valueOf(i)).getAsString();
            matchers[i] = (item.startsWith("#")
                    ? new ItemTagMatcher(item)
                    : new ItemMatcher(Matcher.asItem(item)));
        }
        return new InventoryCraftData(matchers);
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        for (int i = 0; i < matchers.length; i++) {
            json.addProperty(String.valueOf(i), matchers[i].toString());
        }
        return json;
    }

    @Override
    public ArrayList<MutableText> info(EntityPlayerMPFake fakePlayer) {
        // 创建一个集合用来存储可变文本对象，这个集合用来在聊天栏输出多行聊天信息，集合中的每个元素单独占一行
        ArrayList<MutableText> list = new ArrayList<>();
        // 获取假玩家的显示名称
        Text PlayerName = fakePlayer.getDisplayName();
        // 将可变文本“<玩家>正在合成物品，配方:”添加到集合
        list.add(TextUtils.translate("carpet.commands.playerAction.info.craft.result",
                PlayerName, getCraftOutPut(fakePlayer, matchers).getDefaultStack().toHoverableText()));
        // 将可变文本“<玩家>当前合成物品的状态:”添加到集合中
        list.add(TextUtils.translate("carpet.commands.playerAction.info.craft.state", PlayerName));
        // 获取玩家的生存模式物品栏对象
        PlayerScreenHandler playerScreenHandler = fakePlayer.playerScreenHandler;
        // 将每一个合成槽位（包括输出槽位）中的物品的名称和堆叠数组装成一个可变文本对象并添加到集合
        // 合成格第一排
        list.add(TextUtils.appendAll(
                "    ", getWithCountHoverText(playerScreenHandler.getSlot(1).getStack()),
                " ", getWithCountHoverText(playerScreenHandler.getSlot(2).getStack())
        ));
        // 合成格第二排和输出槽
        list.add(TextUtils.appendAll(
                "    ", getWithCountHoverText(playerScreenHandler.getSlot(3).getStack()),
                " ", getWithCountHoverText(playerScreenHandler.getSlot(4).getStack()),
                " -> ", getWithCountHoverText(playerScreenHandler.getSlot(0).getStack())));
        return list;
    }

    public Matcher[] getMatchers() {
        return matchers;
    }
}
