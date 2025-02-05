package org.carpetorgaddition.periodic.fakeplayer.actioncontext;

import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.carpetorgaddition.util.TextUtils;

import java.util.ArrayList;

public abstract class AbstractActionContext {
    public abstract ArrayList<MutableText> info(EntityPlayerMPFake fakePlayer);

    // 获取物品堆栈的可变文本形式：物品名称*堆叠数量
    protected static MutableText getWithCountHoverText(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return TextUtils.hoverText(Text.literal("[A]"), TextUtils.appendAll(Items.AIR.getName()), Formatting.DARK_GRAY);
        }
        // 获取物品堆栈对应的物品ID的首字母，然后转为大写，再放进中括号里
        String capitalizeFirstLetter = getInitial(itemStack);
        return TextUtils.hoverText(
                Text.literal(capitalizeFirstLetter),
                TextUtils.appendAll(itemStack.getItem().getName(), "*" + itemStack.getCount()),
                null
        );
    }

    // 获取物品ID的首字母，然后转为大写，再放进中括号里
    private static String getInitial(ItemStack itemStack) {
        // 将物品名称的字符串切割为命名空间（如果有）和物品id
        String name = Registries.ITEM.getId(itemStack.getItem()).toString();
        String[] split = name.split(":");
        // 获取数组的索引，如果有命名空间，返回1索引，否则返回0索引，即舍弃命名空间
        int index = (split.length == 1) ? 0 : 1;
        // 获取物品id的首字母，然后大写
        return "[" + Character.toUpperCase(split[index].charAt(0)) + "]";
    }

    /**
     * 序列化假玩家动作数据
     */
    public abstract JsonObject toJson();
}
