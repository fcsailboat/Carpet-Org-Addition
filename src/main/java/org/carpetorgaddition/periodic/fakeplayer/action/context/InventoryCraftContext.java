package org.carpetorgaddition.periodic.fakeplayer.action.context;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.wheel.ItemStackPredicate;
import org.carpetorgaddition.util.wheel.TextBuilder;

import java.util.ArrayList;

public class InventoryCraftContext extends AbstractActionContext {
    /**
     * 物品合成所使用的物品栏
     */
    private final ItemStackPredicate[] predicates = new ItemStackPredicate[4];

    public InventoryCraftContext(ItemStackPredicate[] predicates) {
        System.arraycopy(predicates, 0, this.predicates, 0, this.predicates.length);
    }

    public static InventoryCraftContext load(JsonObject json) {
        ItemStackPredicate[] itemStackPredicates = new ItemStackPredicate[4];
        for (int i = 0; i < itemStackPredicates.length; i++) {
            String item = json.get(String.valueOf(i)).getAsString();
            itemStackPredicates[i] = ItemStackPredicate.load(item);
        }
        return new InventoryCraftContext(itemStackPredicates);
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        for (int i = 0; i < predicates.length; i++) {
            json.addProperty(String.valueOf(i), predicates[i].toString());
        }
        return json;
    }

    @Override
    public ArrayList<MutableText> info(EntityPlayerMPFake fakePlayer) {
        // 创建一个集合用来存储可变文本对象，这个集合用来在聊天栏输出多行聊天信息，集合中的每个元素单独占一行
        ArrayList<MutableText> list = new ArrayList<>();
        // 获取假玩家的显示名称
        Text playerName = fakePlayer.getDisplayName();
        // 将可变文本“<玩家>正在合成物品，配方:”添加到集合
        ItemStack craftOutput = ItemStackPredicate.getCraftOutput(predicates, 2, fakePlayer);
        // 如果可以合成物品，返回合成的结果物品，否则返回固定文本“物品”
        Text itemText = craftOutput.isEmpty() ? TextUtils.translate("carpet.command.item.item") : craftOutput.getItem().getName();
        list.add(TextUtils.translate("carpet.commands.playerAction.info.craft.result", playerName, itemText));
        this.addCraftRecipe(list, craftOutput);
        // 将可变文本“<玩家>当前合成物品的状态:”添加到集合中
        list.add(TextUtils.translate("carpet.commands.playerAction.info.craft.state", playerName));
        // 获取玩家的生存模式物品栏对象
        PlayerScreenHandler playerScreenHandler = fakePlayer.playerScreenHandler;
        // 将每一个合成槽位（包括输出槽位）中的物品的名称和堆叠数组装成一个可变文本对象并添加到集合
        addCraftGridState(list, playerScreenHandler);
        return list;
    }

    // 添加合成配方文本
    private void addCraftRecipe(ArrayList<MutableText> list, ItemStack craftOutput) {
        // 配方第一排
        list.add(new TextBuilder()
                .indentation().append(this.predicates[0].getInitialUpperCase())
                .blank().append(this.predicates[1].getInitialUpperCase())
                .toLine());
        // 配方第二排
        TextBuilder builder = new TextBuilder()
                .indentation().append(this.predicates[2].getInitialUpperCase())
                .blank().append(this.predicates[3].getInitialUpperCase());
        if (!craftOutput.isEmpty()) {
            builder.appendString(" -> ").append(AbstractActionContext.getWithCountHoverText(craftOutput));
        }
        list.add(builder.toLine());
    }

    // 合成方格内的物品状态
    private void addCraftGridState(ArrayList<MutableText> list, PlayerScreenHandler playerScreenHandler) {
        // 合成格第一排
        list.add(TextUtils.appendAll(
                "    ", getWithCountHoverText(playerScreenHandler.getSlot(1).getStack()),
                " ", getWithCountHoverText(playerScreenHandler.getSlot(2).getStack())
        ));
        // 合成格第二排和输出槽
        list.add(TextUtils.appendAll(
                "    ", getWithCountHoverText(playerScreenHandler.getSlot(3).getStack()),
                " ", getWithCountHoverText(playerScreenHandler.getSlot(4).getStack()),
                " -> ", getWithCountHoverText(playerScreenHandler.getSlot(0).getStack())
        ));
    }

    public ItemStackPredicate[] getPredicates() {
        return this.predicates;
    }
}
