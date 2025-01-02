package org.carpetorgaddition.periodic.fakeplayer.actiondata;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.item.Items;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.text.MutableText;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.matcher.ItemMatcher;
import org.carpetorgaddition.util.matcher.ItemTagMatcher;
import org.carpetorgaddition.util.matcher.Matcher;

import java.util.ArrayList;

public class CraftingTableCraftData extends AbstractActionData {
    /**
     * 合成所使用的物品栏
     */
    private final Matcher[] matchers = new Matcher[9];

    public CraftingTableCraftData(Matcher[] matchers) {
        System.arraycopy(matchers, 0, this.matchers, 0, this.matchers.length);
    }

    public static CraftingTableCraftData load(JsonObject json) {
        Matcher[] matchers = new Matcher[9];
        for (int i = 0; i < matchers.length; i++) {
            String item = json.get(String.valueOf(i)).getAsString();
            matchers[i] = (item.startsWith("#")
                    ? new ItemTagMatcher(item)
                    : new ItemMatcher(Matcher.asItem(item)));
        }
        return new CraftingTableCraftData(matchers);
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
        // 将可变文本“<玩家>正在合成物品，配方:”添加到集合
        list.add(TextUtils.translate("carpet.commands.playerAction.info.craft.result",
                fakePlayer.getDisplayName(), getCraftOutPut(fakePlayer, matchers).getDefaultStack().toHoverableText()));
        // 判断假玩家是否打开了一个工作台
        if (fakePlayer.currentScreenHandler instanceof CraftingScreenHandler currentScreenHandler) {
            // 将可变文本“<玩家>当前合成物品的状态:”添加到集合中
            list.add(TextUtils.translate("carpet.commands.playerAction.info.craft.state", fakePlayer.getDisplayName()));
            // 如果打开了，将每一个合成槽位（包括输出槽位）中的物品的名称和堆叠数组装成一个可变文本对象并添加到集合
            // 合成格第一排
            list.add(TextUtils.appendAll(
                    "    ", getWithCountHoverText(currentScreenHandler.getSlot(1).getStack()),
                    " ", getWithCountHoverText(currentScreenHandler.getSlot(2).getStack()),
                    " ", getWithCountHoverText(currentScreenHandler.getSlot(3).getStack())));
            // 合成格第二排和输出槽
            list.add(TextUtils.appendAll(
                    "    ", getWithCountHoverText(currentScreenHandler.getSlot(4).getStack()),
                    " ", getWithCountHoverText(currentScreenHandler.getSlot(5).getStack()),
                    " ", getWithCountHoverText(currentScreenHandler.getSlot(6).getStack()),
                    " -> ", getWithCountHoverText(currentScreenHandler.getSlot(0).getStack())));
            // 合成格第三排
            list.add(TextUtils.appendAll(
                    "    ", getWithCountHoverText(currentScreenHandler.getSlot(7).getStack()),
                    " ", getWithCountHoverText(currentScreenHandler.getSlot(8).getStack()),
                    " ", getWithCountHoverText(currentScreenHandler.getSlot(9).getStack())));
        } else {
            // 如果没有打开工作台，将未打开工作台的信息添加到集合
            list.add(TextUtils.translate("carpet.commands.playerAction.info.craft.no_crafting_table",
                    fakePlayer.getDisplayName(), Items.CRAFTING_TABLE.getName()));
        }
        return list;
    }

    public Matcher[] getMatchers() {
        return matchers;
    }
}
