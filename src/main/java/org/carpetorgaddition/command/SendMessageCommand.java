package org.carpetorgaddition.command;

import carpet.utils.CommandHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.ColorArgumentType;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.command.argument.TextArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.util.*;
import org.carpetorgaddition.util.constant.TextConstants;

@SuppressWarnings("SpellCheckingInspection")
public class SendMessageCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess) {
        dispatcher.register(CommandManager.literal("sendMessage")
                .requires(source -> CommandHelper.canUseCommand(source, CarpetOrgAdditionSettings.commandSendMessage))
                .then(CommandManager.literal("copy")
                        .then(CommandManager.argument("text", StringArgumentType.string())
                                .executes(SendMessageCommand::sendReplicableText)))
                .then(CommandManager.literal("url")
                        .then(CommandManager.argument("url", StringArgumentType.string())
                                .executes(context -> sendClickableLink(context, commandRegistryAccess))))
                .then(CommandManager.literal("location")
                        .executes(SendMessageCommand::sendSelfLocation))
                .then(CommandManager.literal("color")
                        .then(CommandManager.argument("color", ColorArgumentType.color())
                                .then(CommandManager.argument("text", StringArgumentType.string())
                                        .executes(SendMessageCommand::sendColorText))))
                .then(CommandManager.literal("strikethrough")
                        .then(CommandManager.argument("text", StringArgumentType.string())
                                .executes(SendMessageCommand::sendStrikethroughText)))
                .then(CommandManager.literal("formatting")
                        .then(CommandManager.argument("text", StringArgumentType.string())
                                .executes(SendMessageCommand::sendFormattingText)))
                .then(CommandManager.literal("item")
                        .executes(context -> SendMessageCommand.sendItemHoverableText(context, true))
                        .then(CommandManager.argument("itemStack", ItemStackArgumentType.itemStack(commandRegistryAccess))
                                .executes(context -> sendItemHoverableText(context, false)))));
    }

    //发送可复制文本
    private static int sendReplicableText(CommandContext<ServerCommandSource> context) {
        //获取命令来源，并进行非空判断
        ServerCommandSource source = context.getSource();
        //在输入命令时输入的消息
        String text = StringArgumentType.getString(context, "text");
        //给文本添加颜色，单击事件，鼠标悬停事件
        MutableText copy = TextUtils.copy(text, text, TextUtils.translate("chat.copy.click"), Formatting.GREEN);
        MessageUtils.broadcastMessage(source.getServer(), appendPlayerName(source, copy));
        return 1;
    }

    //发送可点击链接
    private static int sendClickableLink(CommandContext<ServerCommandSource> context, CommandRegistryAccess commandBuildContext) throws CommandSyntaxException {
        //获取命令来源，并非空判断
        ServerCommandSource source = context.getSource();
        //创建可变文本对象
        String text = StringArgumentType.getString(context, "url");
        TextArgumentType textArgumentType = TextArgumentType.text(commandBuildContext);
        StringReader reader = new StringReader("{text:'',click_event:{action:'open_url',url:'%s'}}".formatted(text));
        // 阻止发送无法解析的网页链接文本
        try {
            textArgumentType.parse(reader);
        } catch (CommandSyntaxException e) {
            throw CommandUtils.createException(e, "carpet.text.url.invalid");
        }
        MutableText url = TextUtils.url(text, text, TextUtils.translate("carpet.commands.sendMessage.url.click_open_url").getString(), null);
        MessageUtils.broadcastMessage(source.getServer(), appendPlayerName(source, url));
        return 1;
    }

    //发送自己的位置
    private static int sendSelfLocation(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        BlockPos blockPos = player.getBlockPos();
        MutableText mutableText = switch (WorldUtils.getDimensionId(player.getWorld())) {
            case "minecraft:overworld" -> TextUtils.translate("carpet.commands.sendMessage.location.overworld",
                    player.getDisplayName(), TextConstants.blockPos(blockPos, Formatting.GREEN),
                    TextConstants.blockPos(MathUtils.getTheNetherPos(player), Formatting.RED));
            case "minecraft:the_nether" -> TextUtils.translate("carpet.commands.sendMessage.location.the_nether",
                    player.getDisplayName(), TextConstants.blockPos(blockPos, Formatting.RED),
                    TextConstants.blockPos(MathUtils.getOverworldPos(player), Formatting.GREEN));
            case "minecraft:the_end" -> TextUtils.translate("carpet.commands.sendMessage.location.the_end",
                    player.getDisplayName(), TextConstants.blockPos(blockPos, Formatting.DARK_PURPLE));
            default -> TextUtils.translate("carpet.commands.sendMessage.location.default",
                    player.getDisplayName(), WorldUtils.getDimensionId(player.getWorld()),
                    TextConstants.blockPos(blockPos, null));
        };
        MessageUtils.broadcastMessage(context.getSource().getServer(), mutableText);
        return 1;
    }

    // 发送带颜色的文本
    private static int sendColorText(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        // 获取文本的颜色
        Formatting color = ColorArgumentType.getColor(context, "color");
        // 获取要发送的文本内容
        String text = StringArgumentType.getString(context, "text");
        // 发送消息
        MessageUtils.broadcastMessage(source.getServer(), appendPlayerName(source, TextUtils.setColor(TextUtils.createText(text), color)));
        return 1;
    }

    // 发送带删除线的消息
    private static int sendStrikethroughText(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        // 获取要发送的文本内容
        String text = StringArgumentType.getString(context, "text");
        // 发送消息
        MessageUtils.broadcastMessage(source.getServer(), appendPlayerName(source,
                TextUtils.regularStyle(text, Formatting.WHITE, false, false, false, true)));
        return 1;
    }

    // 发送可格式化文本
    private static int sendFormattingText(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        // 获取要发送的文本内容，并将“$”符号替换为“§”
        String text = StringArgumentType.getString(context, "text").replaceAll("\\$", "§");
        MessageUtils.broadcastMessage(source.getServer(), appendPlayerName(source, TextUtils.createText(text)));
        return 1;
    }

    // 在文本前添加玩家名（如果玩家不为null）
    private static MutableText appendPlayerName(ServerCommandSource source, MutableText text) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            return text;
        }
        // 如果玩家不为null，则发送消息时在文本前添加玩家名
        return TextUtils.appendAll(player.getDisplayName(), ": ", text);
    }

    // 发送手上的物品的悬停文本
    private static int sendItemHoverableText(CommandContext<ServerCommandSource> context, boolean requiredPlayer) throws CommandSyntaxException {
        ItemStack itemStack;
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        if (requiredPlayer) {
            // 获取玩家主手上的物品
            ItemStack mainHandStack = player.getMainHandStack();
            // 如果玩家主手上的物品为空，就获取玩家副手的物品
            itemStack = mainHandStack.isEmpty() ? player.getOffHandStack() : mainHandStack;
            // 如果副手的物品还是为空，直接抛出异常结束方法
            if (itemStack.isEmpty()) {
                throw CommandUtils.createException("carpet.commands.sendMessage.item.empty");
            }
        } else {
            itemStack = ItemStackArgumentType.getItemStackArgument(context, "itemStack").createStack(1, false);
        }
        // 发送物品带有悬停文本的消息
        MutableText message = TextUtils.appendAll(player.getDisplayName(), "：", itemStack.toHoverableText());
        MessageUtils.broadcastMessage(context.getSource().getServer(), message);
        return 1;
    }
}
