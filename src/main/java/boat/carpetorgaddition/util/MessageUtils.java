package boat.carpetorgaddition.util;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.CarpetOrgAdditionConstants;
import boat.carpetorgaddition.wheel.text.TextBuilder;
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

    public static void sendMessage(PlayerList playerManager, Component message) {
        playerManager.broadcastSystemMessage(message, false);
    }

    public static void sendErrorMessage(MinecraftServer server, Component component, Throwable e) {
        String error = CommandUtils.getExceptionString(e);
        TextBuilder builder = TextBuilder.of(component);
        builder.setHover(error);
        builder.setColor(ChatFormatting.RED);
        sendMessage(server, builder.build());
    }

    public static void sendMessage(ServerPlayer player, Component message) {
        player.sendSystemMessage(message);
        writeLog(PlayerUtils.getName(player), message.getString());
    }

    public static void sendMessage(CommandSourceStack source, Component message) {
        source.sendSystemMessage(message);
        writeLog(source.getTextName(), message.getString());
    }

    private static void writeLog(String name, String message) {
        CarpetOrgAddition.LOGGER.info("({}) [{}] {}", CarpetOrgAdditionConstants.COMPACT_MOD_NAME, name, message);
    }

    public static void sendErrorMessage(CommandSourceStack source, Component message) {
        TextBuilder builder = TextBuilder.of(message);
        builder.setColor(ChatFormatting.RED);
        MessageUtils.sendMessage(source, builder.build());
    }

    public static void sendErrorMessage(ServerPlayer player, Component message, Throwable e) {
        sendErrorMessage(player.createCommandSourceStack(), message, e);
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

    public static void sendMessageToHud(Player player, Component message) {
        player.sendOverlayMessage(message);
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

    public static void sendErrorMessageToHudIfPlayer(CommandSourceStack source, CommandSyntaxException e) {
        sendMessageToHudIfPlayer(source, () -> {
            TextBuilder builder = TextBuilder.of(e.getRawMessage());
            builder.setColor(ChatFormatting.RED);
            return builder.build();
        });
    }

    /**
     * 发送多条消息，每一条消息单独占一行，消息内容仅发送者可见
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
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            sendEmptyMessage(player);
        } else {
            sendMessage(source, TextBuilder.empty());
        }
    }

    public static void sendEmptyMessage(MinecraftServer server) {
        sendMessage(server, TextBuilder.empty());
    }
}
