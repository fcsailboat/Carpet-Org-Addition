package org.carpetorgaddition.periodic.fakeplayer.action.context;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.wheel.ItemStackPredicate;
import org.carpetorgaddition.util.wheel.TextBuilder;

import java.util.ArrayList;

public class CraftingTableCraftContext extends AbstractActionContext {
    /**
     * 合成配方
     */
    private final ItemStackPredicate[] predicates = new ItemStackPredicate[9];

    public CraftingTableCraftContext(ItemStackPredicate[] predicates) {
        System.arraycopy(predicates, 0, this.predicates, 0, this.predicates.length);
    }

    public static CraftingTableCraftContext load(JsonObject json) {
        ItemStackPredicate[] predicates = new ItemStackPredicate[9];
        for (int i = 0; i < predicates.length; i++) {
            String item = json.get(String.valueOf(i)).getAsString();
            predicates[i] = ItemStackPredicate.load(item);
        }
        return new CraftingTableCraftContext(predicates);
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
        // 将可变文本“<玩家>正在合成物品，配方:”添加到集合
        ItemStack craftOutput = ItemStackPredicate.getCraftOutput(predicates, 3, fakePlayer);
        // 如果可以合成物品，返回合成的结果物品，否则返回固定文本“物品”
        Text itemText = craftOutput.isEmpty() ? TextUtils.translate("carpet.command.item.item") : craftOutput.getItem().getName();
        list.add(TextUtils.translate("carpet.commands.playerAction.info.craft.result", fakePlayer.getDisplayName(), itemText));
        this.addCraftRecipe(list, craftOutput);
        // 判断假玩家是否打开了一个工作台
        if (fakePlayer.currentScreenHandler instanceof CraftingScreenHandler currentScreenHandler) {
            // 将可变文本“<玩家>当前合成物品的状态:”添加到集合中
            list.add(TextUtils.translate("carpet.commands.playerAction.info.craft.state", fakePlayer.getDisplayName()));
            // 如果打开了，将每一个合成槽位（包括输出槽位）中的物品的名称和堆叠数组装成一个可变文本对象并添加到集合
            addCraftGridState(currentScreenHandler, list);
        } else {
            // 如果没有打开工作台，将未打开工作台的信息添加到集合
            list.add(TextUtils.translate("carpet.commands.playerAction.info.craft.no_crafting_table",
                    fakePlayer.getDisplayName(), Items.CRAFTING_TABLE.getName()));
        }
        return list;
    }

    private void addCraftRecipe(ArrayList<MutableText> list, ItemStack craftOutput) {
        // 配方第一排
        list.add(new TextBuilder()
                .indentation().append(this.predicates[0].getInitialUpperCase())
                .blank().append(this.predicates[1].getInitialUpperCase())
                .blank().append(this.predicates[2].getInitialUpperCase())
                .toLine());
        // 配方第二排
        TextBuilder builder = new TextBuilder()
                .indentation().append(this.predicates[3].getInitialUpperCase())
                .blank().append(this.predicates[4].getInitialUpperCase())
                .blank().append(this.predicates[5].getInitialUpperCase());
        if (!craftOutput.isEmpty()) {
            builder.appendString(" -> ").append(AbstractActionContext.getWithCountHoverText(craftOutput));
        }
        list.add(builder.toLine());
        // 配方第三排
        list.add(new TextBuilder()
                .indentation().append(this.predicates[6].getInitialUpperCase())
                .blank().append(this.predicates[7].getInitialUpperCase())
                .blank().append(this.predicates[8].getInitialUpperCase())
                .toLine());
    }

    // 添加当前合成方格的状态
    private void addCraftGridState(CraftingScreenHandler currentScreenHandler, ArrayList<MutableText> list) {
        // 合成格第一排
        list.add(TextUtils.appendAll(
                "    ", getWithCountHoverText(currentScreenHandler.getSlot(1).getStack()),
                " ", getWithCountHoverText(currentScreenHandler.getSlot(2).getStack()),
                " ", getWithCountHoverText(currentScreenHandler.getSlot(3).getStack())
        ));
        // 合成格第二排和输出槽
        list.add(TextUtils.appendAll(
                "    ", getWithCountHoverText(currentScreenHandler.getSlot(4).getStack()),
                " ", getWithCountHoverText(currentScreenHandler.getSlot(5).getStack()),
                " ", getWithCountHoverText(currentScreenHandler.getSlot(6).getStack()),
                " -> ", getWithCountHoverText(currentScreenHandler.getSlot(0).getStack())
        ));
        // 合成格第三排
        list.add(TextUtils.appendAll(
                "    ", getWithCountHoverText(currentScreenHandler.getSlot(7).getStack()),
                " ", getWithCountHoverText(currentScreenHandler.getSlot(8).getStack()),
                " ", getWithCountHoverText(currentScreenHandler.getSlot(9).getStack())
        ));
    }

    public ItemStackPredicate[] getPredicates() {
        return this.predicates;
    }
}
