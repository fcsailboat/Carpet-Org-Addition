package org.carpetorgaddition.periodic.fakeplayer.action.context;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.wheel.ItemStackPredicate;

import java.util.ArrayList;

public class FillContext extends AbstractActionContext {
    private static final String ITEM = "item";
    private static final String ALL_ITEM = "allItem";
    public static final FillContext FILL_ALL = new FillContext(null, true);
    /**
     * 要向潜影盒填充的物品
     */
    private final Item item;
    /**
     * 是否向潜影盒内填充任意物品并忽略{@link FillContext#item}（本身就不能放入潜影盒的物品不会被填充）
     */
    private final boolean allItem;

    public FillContext(Item item, boolean allItem) {
        this.item = item;
        this.allItem = allItem;
    }

    public static FillContext load(JsonObject json) {
        boolean allItem = json.get(ALL_ITEM).getAsBoolean();
        if (allItem) {
            return new FillContext(null, true);
        }
        Item item = ItemStackPredicate.stringAsItem(json.get(ITEM).getAsString());
        return new FillContext(item, false);
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
            // 将“<玩家名> 正在向 潜影盒 填充 [item] 物品”信息添加到集合
            list.add(TextUtils.translate("carpet.commands.playerAction.info.fill_all.item",
                    fakePlayer.getDisplayName(), Items.SHULKER_BOX.getName()));
        } else {
            // 将“<玩家名> 正在向 潜影盒 填充 [item] 物品”信息添加到集合
            list.add(TextUtils.translate("carpet.commands.playerAction.info.fill.item",
                    fakePlayer.getDisplayName(), Items.SHULKER_BOX.getName(), this.item.getDefaultStack().toHoverableText()));
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
