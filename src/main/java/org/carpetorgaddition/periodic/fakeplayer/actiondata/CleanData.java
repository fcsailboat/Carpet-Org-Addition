package org.carpetorgaddition.periodic.fakeplayer.actiondata;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.matcher.Matcher;

import java.util.ArrayList;

public class CleanData extends AbstractActionData {
    private static final String ITEM = "item";
    private static final String ALL_ITEM = "allItem";
    public static final CleanData CLEAN_ALL = new CleanData(null, true);
    /**
     * 要从潜影盒中丢出的物品
     */
    private final Item item;
    /**
     * 是否忽略{@link CleanData#item}，并清空潜影盒内的所有物品
     */
    private final boolean allItem;

    /**
     * @param item    要清空的物品
     * @param allItem 是否清空所有物品
     */
    public CleanData(Item item, boolean allItem) {
        this.item = item;
        this.allItem = allItem;
    }

    public static CleanData load(JsonObject json) {
        boolean allItem = json.get(ALL_ITEM).getAsBoolean();
        if (allItem) {
            return new CleanData(null, true);
        }
        Item item = Matcher.asItem(json.get(ITEM).getAsString());
        return new CleanData(item, false);
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        if (this.item != null) {
            // 要清空的物品
            json.addProperty(ITEM, Registries.ITEM.getId(this.item).toString());
        }
        json.addProperty(ALL_ITEM, this.allItem);
        return json;
    }

    @Override
    public ArrayList<MutableText> info(EntityPlayerMPFake fakePlayer) {
        ArrayList<MutableText> list = new ArrayList<>();
        if (this.allItem) {
            // 将玩家清空潜影盒的信息添加到集合
            list.add(TextUtils.translate("carpet.commands.playerAction.info.clean.item",
                    fakePlayer.getDisplayName(),
                    Items.SHULKER_BOX.getName()));
        } else {
            // 将玩家清空潜影盒的信息添加到集合
            list.add(TextUtils.translate("carpet.commands.playerAction.info.clean.designated_item",
                    fakePlayer.getDisplayName(),
                    Items.SHULKER_BOX.getName(),
                    this.item.getName()));
        }
        return list;
    }

    public Item getItem() {
        return item;
    }

    public boolean isAllItem() {
        return allItem;
    }
}
