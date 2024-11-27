package org.carpetorgaddition.command;

import carpet.patches.EntityPlayerMPFake;
import carpet.utils.CommandHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.exception.CommandExecuteIOException;
import org.carpetorgaddition.util.CommandUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.TextUtils;
import org.carpetorgaddition.util.express.Express;
import org.carpetorgaddition.util.express.ExpressManager;
import org.carpetorgaddition.util.express.ExpressManagerInterface;
import org.carpetorgaddition.util.screen.ShipExpressScreenHandler;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MailCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("mail")
                .requires(source -> CommandHelper.canUseCommand(source, CarpetOrgAdditionSettings.commandMail))
                .then(CommandManager.literal("ship")
                        .then(CommandManager.argument(CommandUtils.PLAYER, EntityArgumentType.player())
                                .executes(MailCommand::ship)))
                .then(CommandManager.literal("receive")
                        .executes(MailCommand::receiveAll)
                        .then(CommandManager.argument("id", IntegerArgumentType.integer(1))
                                .suggests(receiveSuggests(true))
                                .executes(MailCommand::receive)))
                .then(CommandManager.literal("cancel")
                        .executes(MailCommand::cancelAll)
                        .then(CommandManager.argument("id", IntegerArgumentType.integer(1))
                                .suggests(receiveSuggests(false))
                                .executes(MailCommand::cancel)))
                .then(CommandManager.literal("list")
                        .executes(MailCommand::list))
                .then(CommandManager.literal("multiple")
                        .then(CommandManager.argument(CommandUtils.PLAYER, EntityArgumentType.player())
                                .executes(MailCommand::shipMultipleExpress))));
    }

    // 自动补全快递单号
    private static @NotNull SuggestionProvider<ServerCommandSource> receiveSuggests(boolean recipient) {
        return (context, builder) -> {
            ServerPlayerEntity player = context.getSource().getPlayer();
            if (player == null) {
                return CommandSource.suggestMatching(List.of(), builder);
            }
            MinecraftServer server = context.getSource().getServer();
            ExpressManager expressManager = ExpressManagerInterface.getInstance(server);
            // 获取所有发送给自己的快递（或所有自己发送的快递）
            List<String> list = expressManager.stream()
                    .filter(express -> recipient ? express.isRecipient(player) : express.isSender(player))
                    .map(express -> Integer.toString(express.getId())).toList();
            return CommandSource.suggestMatching(list, builder);
        };
    }

    // 发送快递
    private static int ship(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity sourcePlayer = CommandUtils.getSourcePlayer(context);
        ServerPlayerEntity targetPlayer = CommandUtils.getArgumentPlayer(context);
        // 限制只允许发送给其他真玩家
        checkPlayer(sourcePlayer, targetPlayer);
        MinecraftServer server = context.getSource().getServer();
        ExpressManager expressManager = ExpressManagerInterface.getInstance(server);
        try {
            // 将快递信息添加到快递管理器
            expressManager.put(new Express(server, sourcePlayer, targetPlayer, expressManager.generateNumber()));
        } catch (IOException e) {
            throw CommandExecuteIOException.of(e);
        }
        return 1;
    }

    // 发送多个快递
    private static int shipMultipleExpress(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity sourcePlayer = CommandUtils.getSourcePlayer(context);
        ServerPlayerEntity targetPlayer = CommandUtils.getArgumentPlayer(context);
        checkPlayer(sourcePlayer, targetPlayer);
        SimpleInventory inventory = new SimpleInventory(27);
        SimpleNamedScreenHandlerFactory screen = new SimpleNamedScreenHandlerFactory((i, inv, player)
                -> new ShipExpressScreenHandler(i, inv, sourcePlayer, targetPlayer, inventory),
                TextUtils.translate("carpet.commands.multiple.gui"));
        sourcePlayer.openHandledScreen(screen);
        return 1;
    }

    // 接收快递
    private static int receive(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        // 获取快递
        Express express = getExpress(context);
        // 只能接收发送给自己的快递
        if (express.isRecipient(player)) {
            try {
                express.receive();
            } catch (IOException e) {
                throw CommandExecuteIOException.of(e);
            }
            return 1;
        }
        throw CommandUtils.createException("carpet.commands.mail.receive.recipient");
    }

    // 接收所有快递
    private static int receiveAll(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        ExpressManager expressManager = ExpressManagerInterface.getInstance(context);
        try {
            return expressManager.receiveAll(player);
        } catch (IOException e) {
            throw CommandExecuteIOException.of(e);
        }
    }

    // 撤回快递
    private static int cancel(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        Express express = getExpress(context);
        if (express.isSender(player)) {
            try {
                express.cancel();
            } catch (IOException e) {
                throw CommandExecuteIOException.of(e);
            }
            return 1;
        }
        throw CommandUtils.createException("carpet.commands.mail.cancel.recipient");
    }

    // 撤回所有快递
    private static int cancelAll(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        ExpressManager expressManager = ExpressManagerInterface.getInstance(context);
        try {
            return expressManager.cancelAll(player);
        } catch (IOException e) {
            throw CommandExecuteIOException.of(e);
        }
    }

    // 列出快递
    private static int list(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        final ServerPlayerEntity player = CommandUtils.getSourcePlayer(context);
        MinecraftServer server = context.getSource().getServer();
        ExpressManager expressManager = ExpressManagerInterface.getInstance(server);
        List<Express> list = expressManager.stream().toList();
        if (list.isEmpty()) {
            // 没有快递被列出
            MessageUtils.sendMessage(context, "carpet.commands.mail.list.empty");
        }
        list.forEach(express -> list(player, express));
        return list.size();
    }

    private static void list(ServerPlayerEntity player, Express express) {
        ArrayList<MutableText> list = new ArrayList<>();
        MutableText text;
        if (express.isRecipient(player)) {
            text = TextUtils.createText("[R]");
            // 点击接收
            TextUtils.command(text, "/mail receive " + express.getId(), null, Formatting.AQUA, false);
            list.add(TextUtils.translate("carpet.commands.mail.list.receive"));
            list.add(TextUtils.createEmpty());
        } else if (express.isSender(player)) {
            text = TextUtils.createText("[C]");
            // 点击撤回
            TextUtils.command(text, "/mail cancel " + express.getId(), null, Formatting.AQUA, false);
            list.add(TextUtils.translate("carpet.commands.mail.list.sending"));
            list.add(TextUtils.createEmpty());
        } else {
            text = TextUtils.createText("[?]");
        }
        list.add(TextUtils.translate("carpet.commands.mail.list.id", express.getId()));
        list.add(TextUtils.translate("carpet.commands.mail.list.sender", express.getSender()));
        list.add(TextUtils.translate("carpet.commands.mail.list.recipient", express.getRecipient()));
        list.add(TextUtils.translate("carpet.commands.mail.list.item",
                TextUtils.translate(express.getExpress().getItem().getTranslationKey()), express.getExpress().getCount()));
        list.add(TextUtils.translate("carpet.commands.mail.list.time", express.getTime()));
        // 拼接字符串
        text = TextUtils.hoverText(text, TextUtils.appendList(list));
        MessageUtils.sendMessage(player.getCommandSource(), "carpet.commands.mail.list.each",
                express.getId(), express.getExpress().toHoverableText(), express.getSender(), express.getRecipient(), text);
    }

    // 获取快递
    private static @NotNull Express getExpress(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        MinecraftServer server = context.getSource().getServer();
        ExpressManager expressManager = ExpressManagerInterface.getInstance(server);
        // 获取快递单号
        int id = IntegerArgumentType.getInteger(context, "id");
        // 查找指定单号的快递
        Optional<Express> optional = expressManager.binarySearch(id);
        if (optional.isEmpty()) {
            throw CommandUtils.createException("carpet.commands.mail.receive.non_existent", id);
        }
        return optional.get();
    }

    // 检查玩家是否是自己或假玩家
    private static void checkPlayer(ServerPlayerEntity sourcePlayer, ServerPlayerEntity targetPlayer) throws CommandSyntaxException {
        // 允许在开发环境下发送给自己
        if (CarpetOrgAddition.isDebugDevelopment()) {
            return;
        }
        if (sourcePlayer == targetPlayer || targetPlayer instanceof EntityPlayerMPFake) {
            throw CommandUtils.createException("carpet.commands.mail.check_player");
        }
    }
}
