package org.carpetorgaddition.util.constant;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.WorldUtils;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.ArrayList;

public class TextConstants {
    /**
     * 主世界
     */
    public static final Text OVERWORLD = TextUtils.translate("carpet.command.dimension.overworld");
    /**
     * 下界
     */
    public static final Text THE_NETHER = TextUtils.translate("carpet.command.dimension.the_nether");
    /**
     * 末地
     */
    public static final Text THE_END = TextUtils.translate("carpet.command.dimension.the_end");
    public static final Text TRUE = TextUtils.translate("carpet.command.boolean.true");
    public static final Text FALSE = TextUtils.translate("carpet.command.boolean.false");
    /**
     * [这里]
     */
    public static final Text CLICK_HERE = TextUtils.translate("carpet.command.text.click.here");
    /**
     * 物品
     */
    public static final Text ITEM = TextUtils.translate("carpet.command.item.item");

    public static Text getBoolean(boolean value) {
        return value ? TRUE : FALSE;
    }

    /**
     * 获取一个方块坐标的可变文本对象，并带有点击复制、悬停文本，颜色效果
     *
     * @param color 文本的颜色，如果为null，不修改颜色
     */
    public static MutableText blockPos(BlockPos blockPos, @Nullable Formatting color) {
        MutableText pos = simpleBlockPos(blockPos);
        //添加单击事件，复制方块坐标
        pos.styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, WorldUtils.toPosString(blockPos))));
        //添加光标悬停事件：单击复制到剪贴板
        pos.styled(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextUtils.translate("chat.copy.click"))));
        if (color != null) {
            //修改文本颜色
            pos.styled(style -> style.withColor(color));
        }
        return switch (CarpetOrgAdditionSettings.canHighlightBlockPos) {
            case FALSE -> pos;
            case OMMC -> {
                MutableText highlight = TextUtils.createText(" [H]");
                TextUtils.command(highlight, "/highlightWaypoint " + WorldUtils.toPosString(blockPos),
                        TextUtils.translate("ommc.highlight_waypoint.tooltip"), color, false);
                yield TextUtils.appendAll(pos, highlight);
            }
            case DEFAULT -> {
                MutableText highlight = TextUtils.createText(" [H]");
                TextUtils.command(highlight, "/highlight " + WorldUtils.toPosString(blockPos),
                        TextUtils.translate("carpet.client.commands.highlight"), color, false);
                yield TextUtils.appendAll(pos, highlight);
            }
        };
    }

    /**
     * 返回一个简单的没有任何样式的方块坐标可变文本对象
     */
    public static MutableText simpleBlockPos(BlockPos blockPos) {
        return Texts.bracketed(Text.translatable("chat.coordinates", blockPos.getX(), blockPos.getY(), blockPos.getZ()));
    }

    /**
     * 单击输入"{@code 命令}"
     */
    public static MutableText clickInput(String command) {
        return TextUtils.translate("carpet.command.text.click.input", command);
    }

    /**
     * 单击执行{@code 命令}
     *
     * @param command 要执行的命令
     */
    public static MutableText clickRun(String command) {
        MutableText run = CLICK_HERE.copy();
        // 文本的悬停提示
        MutableText hoverText = TextUtils.translate("carpet.command.text.click.run", command);
        return TextUtils.command(run, command, hoverText, Formatting.AQUA, false);
    }

    /**
     * 返回物品有几组几个
     *
     * @return {@code 物品组数}组{@code 物品个数}个
     */
    public static MutableText itemCount(int count, int maxCount) {
        // 计算物品有多少组
        int group = count / maxCount;
        // 计算物品余几个
        int remainder = count % maxCount;
        MutableText text = TextUtils.createText(String.valueOf(count));
        // 为文本添加悬停提示
        if (group == 0) {
            return TextUtils.hoverText(text, TextUtils.translate("carpet.command.item.remainder", remainder));
        } else if (remainder == 0) {
            return TextUtils.hoverText(text, TextUtils.translate("carpet.command.item.group", group));
        } else {
            return TextUtils.hoverText(text, TextUtils.translate("carpet.command.item.count", group, remainder));
        }
    }

    /**
     * @param base 原始的文本对象
     * @return 获取物品栏中物品的名称和堆叠数量并用“*”连接，每个物品独占一行
     */
    public static MutableText inventory(Text base, Inventory inventory) {
        ArrayList<Text> list = new ArrayList<>();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack itemStack = inventory.getStack(i);
            if (itemStack.isEmpty()) {
                continue;
            }
            list.add(TextUtils.appendAll(itemStack.getName(), "*", String.valueOf(itemStack.getCount())));
        }
        return TextUtils.hoverText(base, TextUtils.appendList(list));
    }

    /**
     * 将游戏刻时间转换为几分几秒的形式，如果时间非常接近整点，例如一小时零一秒，则会直接返回一小时，多出来的一秒会被忽略
     *
     * @param tick 游戏刻时间
     */
    public static MutableText tickToTime(long tick) {
        // 游戏刻
        if (tick < 20L) {
            return TextUtils.translate("carpet.command.time.tick", tick);
        }
        // 秒
        if (tick < 1200L) {
            return TextUtils.translate("carpet.command.time.second", tick / 20L);
        }
        // 整分
        if (tick < 72000L && (tick % 1200L == 0 || (tick / 20L) % 60L == 0)) {
            return TextUtils.translate("carpet.command.time.minute", tick / 1200L);
        }
        // 分和秒
        if (tick < 72000L) {
            return TextUtils.translate("carpet.command.time.minute_second", tick / 1200L, (tick / 20L) % 60L);
        }
        // 整小时
        if (tick % 72000L == 0 || (tick / 20L / 60L) % 60L == 0) {
            return TextUtils.translate("carpet.command.time.hour", tick / 72000L);
        }
        // 小时和分钟
        return TextUtils.translate("carpet.command.time.hour_minute", tick / 72000L, (tick / 20L / 60L) % 60L);
    }

    /**
     * 将当前系统时间偏移指定游戏刻数后返回时间的年月日时分秒形式
     *
     * @param offset 时间偏移的游戏刻数
     * @return 指定游戏刻之后的时间
     */
    public static MutableText tickToRealTime(long offset) {
        LocalDateTime time = LocalDateTime.now().plusSeconds(offset / 20);
        return TextUtils.translate("carpet.command.time.format",
                time.getYear(), time.getMonth().ordinal() + 1, time.getDayOfMonth(),
                time.getHour(), time.getMinute(), time.getSecond());
    }
}
