package boat.carpetorgaddition.wheel.provider;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.util.MathUtils;
import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.LocalizationKeys.Dimension;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import boat.carpetorgaddition.wheel.text.TextJoiner;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

public class TextProvider {
    /**
     * 换行
     */
    public static final Component NEW_LINE = TextBuilder.create("\n");

    private TextProvider() {
    }

    /**
     * 单击复制到剪贴板
     *
     * @apiNote 玩家客户端中一定有这条消息，不需要回调
     */
    public static final Component COPY_CLICK = Component.translatable("chat.copy.click");

    public static Component getBoolean(boolean value) {
        return (value ? LocalizationKeys.Literal.TRUE : LocalizationKeys.Literal.FALSE).translate();
    }

    public static Component blockPos(BlockPos blockPos) {
        return blockPos(blockPos, ChatFormatting.GREEN);
    }

    /**
     * 获取一个方块坐标的可变文本对象，并带有点击复制、悬停文本，颜色效果
     *
     * @param color 文本的颜色，如果为null，不修改颜色
     */
    public static Component blockPos(BlockPos blockPos, @Nullable ChatFormatting color) {
        TextJoiner joiner = new TextJoiner();
        joiner.append(
                TextBuilder.of(simpleBlockPos(blockPos))
                        .setCopyToClipboard(ServerUtils.toPosString(blockPos))
                        .build()
        );
        switch (CarpetOrgAdditionSettings.CAN_HIGHLIGHT_BLOCK_POS.value()) {
            case OMMC -> joiner.append(
                    TextBuilder.of(" [H]")
                            .setCommand(CommandProvider.highlightWaypointByOmmc(blockPos))
                            .setHover(LocalizationKey.literal("ommc.highlight_waypoint.tooltip").translate())
                            .build()
            );
            case DEFAULT -> joiner.append(
                    TextBuilder.of(" [H]")
                            .setCommand(CommandProvider.highlightWaypoint(blockPos))
                            .setHover(LocalizationKeys.Button.HIGHLIGHT.translate())
                            .build()
            );
            default -> {
            }
        }
        TextBuilder builder = TextBuilder.of(joiner.join());
        builder.setColor(color);
        return builder.build();
    }

    /**
     * 返回一个简单的没有任何样式的方块坐标可变文本对象
     */
    public static Component simpleBlockPos(BlockPos blockPos) {
        return ComponentUtils.wrapInSquareBrackets(Component.translatable("chat.coordinates", blockPos.getX(), blockPos.getY(), blockPos.getZ()));
    }

    /**
     * 单击执行命令
     *
     * @param command 要执行的命令
     */
    public static Component clickRun(String command) {
        TextBuilder builder = TextBuilder.of(LocalizationKeys.Button.HERE.translate());
        builder.setCommand(command);
        builder.setHover(LocalizationKeys.Button.RUN_COMMAND.translate(command));
        builder.setColor(ChatFormatting.AQUA);
        return builder.build();
    }

    /**
     * 返回物品有几组几个
     *
     * @return {@code 物品组数}组{@code 物品个数}个
     */
    public static Component itemCount(int count, int maxCount) {
        // 计算物品有多少组
        int group = count / maxCount;
        // 计算物品余几个
        int remainder = count % maxCount;
        TextBuilder builder = TextBuilder.of(count);
        // 为文本添加悬停提示
        if (group == 0) {
            builder.setHover(LocalizationKeys.Item.REMAINDER.translate(remainder));
        } else if (remainder == 0) {
            builder.setHover(LocalizationKeys.Item.GROUP.translate(group));
        } else {
            builder.setHover(LocalizationKeys.Item.COUNT.translate(group, remainder));
        }
        return builder.build();
    }

    /**
     * 将游戏刻时间转换为几分几秒的形式，如果时间非常接近整点，例如一小时零一秒，则会直接返回一小时，多出来的一秒会被忽略
     *
     * @param tick 游戏刻时间
     */
    public static Component tickToTime(long tick) {
        // 游戏刻
        if (tick < 20L) {
            return LocalizationKeys.Time.TICK.translate(tick);
        }
        // 秒
        if (tick < 1200L) {
            return LocalizationKeys.Time.SECOND.translate(tick / 20L);
        }
        // 整分
        if (tick < 72000L && (tick % 1200L == 0 || (tick / 20L) % 60L == 0)) {
            return LocalizationKeys.Time.MINUTE.translate(tick / 1200L);
        }
        // 分和秒
        if (tick < 72000L) {
            return LocalizationKeys.Time.MINUTE_SECOND.translate(tick / 1200L, (tick / 20L) % 60L);
        }
        // 整小时
        if (tick % 72000L == 0 || (tick / 20L / 60L) % 60L == 0) {
            return LocalizationKeys.Time.HOUR.translate(tick / 72000L);
        }
        // 小时和分钟
        return LocalizationKeys.Time.HOUR_MINUTE.translate(tick / 72000L, (tick / 20L / 60L) % 60L);
    }

    /**
     * 将当前系统时间偏移指定游戏刻数后返回时间的年月日时分秒形式
     *
     * @param offset 时间偏移的游戏刻数
     * @return 指定游戏刻之后的时间
     */
    public static Component tickToRealTime(long offset) {
        LocalDateTime time = LocalDateTime.now().plusSeconds(offset / 20);
        return LocalizationKeys.Time.FORMAT.translate(
                time.getYear(),
                time.getMonth().ordinal() + 1,
                time.getDayOfMonth(),
                time.getHour(),
                time.getMinute(),
                time.getSecond()
        );
    }

    /**
     * 将一个浮点数转换为百分比样式，百分数结尾的0会被抹除
     */
    public static Component percentage(double value) {
        String percentage = MathUtils.formatToMaxTwoDecimals(100 * value);
        return TextBuilder.create(percentage + "%");
    }

    /**
     * 获取维度名称
     *
     * @param world 要获取维度名称的世界对象
     * @return 如果是原版的3个维度，返回本Mod翻译后的名称，否则自己返回维度ID
     */
    public static Component dimension(Level world) {
        String dimension = ServerUtils.getIdAsString(world);
        return switch (dimension) {
            case ServerUtils.OVERWORLD -> Dimension.OVERWORLD.translate();
            case ServerUtils.THE_NETHER -> Dimension.THE_NETHER.translate();
            case ServerUtils.THE_END -> Dimension.THE_END.translate();
            default -> TextBuilder.create(dimension);
        };
    }

    public static Component dimension(String dimension) {
        return switch (dimension) {
            case ServerUtils.OVERWORLD, ServerUtils.SIMPLE_OVERWORLD -> Dimension.OVERWORLD.translate();
            case ServerUtils.THE_NETHER, ServerUtils.SIMPLE_THE_NETHER -> Dimension.THE_NETHER.translate();
            case ServerUtils.THE_END, ServerUtils.SIMPLE_THE_END -> Dimension.THE_END.translate();
            default -> TextBuilder.create(dimension);
        };
    }
}
