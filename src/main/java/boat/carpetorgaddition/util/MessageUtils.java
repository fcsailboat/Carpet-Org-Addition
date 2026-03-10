package boat.carpetorgaddition.util;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.CarpetOrgAdditionConstants;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class MessageUtils {
    private MessageUtils() {
    }

    public static void sendMessage(MinecraftServer server, Component message) {
        sendMessage(server.getPlayerList(), message);
    }

    /**
     * 广播一条带有特殊样式的文本消息
     *
     * @param playerManager 通过这个玩家管理器对象发送消息
     * @param message       要广播消息的内容
     */
    public static void sendMessage(PlayerList playerManager, Component message) {
        playerManager.broadcastSystemMessage(message, false);
    }

    /**
     * 广播一条错误消息
     */
    public static void sendErrorMessage(MinecraftServer server, Component component, Throwable e) {
        String error = CommandUtils.getExceptionString(e);
        TextBuilder builder = TextBuilder.of(component);
        builder.setHover(error);
        builder.setColor(ChatFormatting.RED);
        sendMessage(server, builder.build());
    }

    /**
     * 让一个玩家发送带有特殊样式的文本，文本内容仅对消息发送者可见
     *
     * @param player  要发送文本消息的玩家
     * @param message 发送文本消息的内容
     */
    public static void sendMessage(ServerPlayer player, Component message) {
        player.sendSystemMessage(message);
        writeLog(PlayerUtils.getName(player), message.getString());
    }

    /**
     * 让一个玩家发送带有特殊样式的文本，文本内容仅对消息发送者可见
     *
     * @param source  要发送文本消息的命令源
     * @param message 发送文本消息的内容
     */
    public static void sendMessage(CommandSourceStack source, Component message) {
        source.sendSystemMessage(message);
        writeLog(source.getTextName(), message.getString());
    }

    public static void sendMessage(CommandContext<CommandSourceStack> context, Component message) {
        sendMessage(context.getSource(), message);
    }


    private static void writeLog(String name, String message) {
        CarpetOrgAddition.LOGGER.info("({}) [{}] {}", CarpetOrgAdditionConstants.COMPACT_MOD_NAME, name, message);
    }

    public static void sendErrorMessage(CommandSourceStack source, Component message) {
        TextBuilder builder = TextBuilder.of(message);
        builder.setColor(ChatFormatting.RED);
        MessageUtils.sendMessage(source, builder.build());
    }

    public static void sendErrorMessage(CommandSourceStack source, Component message, Throwable e) {
        String error = CommandUtils.getExceptionString(e);
        TextBuilder builder = TextBuilder.of(message);
        builder.setColor(ChatFormatting.RED);
        builder.setHover(error);
        MessageUtils.sendMessage(source, builder.build());
    }

    public static void sendVanillaErrorMessage(CommandSourceStack source, CommandSyntaxException e) {
        source.sendFailure(TextBuilder.create(e.getRawMessage()));
    }

    /**
     * 让一个玩家发送带有特殊样式的文本，文本会显示在屏幕中下方的HUD上，文本内容仅对消息发送者可见
     *
     * @param player  要发送文本消息的玩家
     * @param message 发送文本消息的内容
     */
    public static void sendMessageToHud(Player player, Component message) {
        player.sendOverlayMessage(message);
    }

    /**
     * 如果是玩家，则向HUD发送消息
     */
    @SuppressWarnings("unused")
    public static void sendMessageToHudIfPlayer(CommandSourceStack source, Component message) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return;
        }
        sendMessageToHud(player, message);
    }

    public static void sendMessageToHudIfPlayer(CommandSourceStack source, Supplier<Component> supplier) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return;
        }
        sendMessageToHud(player, supplier.get());
    }

    public static void sendMessageIfPlayerOnline(MinecraftServer server, String name, Supplier<Component> supplier) {
        ServerPlayer player = server.getPlayerList().getPlayerByName(name);
        if (player == null) {
            return;
        }
        sendMessage(player, supplier.get());
    }

    public static void sendMessageIfPlayerOnline(MinecraftServer server, String name, Function<ServerPlayer, Component> function) {
        ServerPlayer player = server.getPlayerList().getPlayerByName(name);
        if (player == null) {
            return;
        }
        sendMessage(player, function.apply(player));
    }

    public static void sendErrorMessageToHud(CommandSourceStack source, CommandSyntaxException e) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return;
        }
        TextBuilder builder = TextBuilder.of(e.getRawMessage());
        builder.setColor(ChatFormatting.RED);
        sendMessageToHud(player, builder.build());
    }

    /**
     * 发送多条带有特殊样式的消息，每一条消息单独占一行，消息内容仅发送者可见
     *
     * @param source 消息的发送者，消息内容仅发送者可见
     * @param list   存储所有要发送的消息的集合
     */
    public static void sendListMessage(CommandSourceStack source, List<Component> list) {
        for (Component message : list) {
            sendMessage(source, message);
        }
    }

    /**
     * 发送一条空白消息
     *
     * @apiNote 用于在聊天栏中分隔消息内容
     */
    public static void sendEmptyMessage(ServerPlayer player) {
        sendMessage(player, Component.empty());
    }

    public static void sendEmptyMessage(CommandSourceStack source) {
        sendEmptyMessage(source.getPlayer());
    }

    public static void sendEmptyMessage(CommandContext<CommandSourceStack> context) {
        sendEmptyMessage(context.getSource());
    }
}
