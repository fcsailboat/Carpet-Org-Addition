package org.carpetorgaddition.util;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Objects;

public class MessageUtils {
    private MessageUtils() {
    }

    /**
     * 广播一条带有特殊样式的文本消息
     *
     * @param player  通过这个玩家对象获取玩家管理器对象，然后通过玩家管理器对象发送消息，player不是消息的发送者
     * @param message 要广播消息的内容
     * @deprecated 使用玩家做为参数有误导性
     */
    @Deprecated(forRemoval = true)
    public static void broadcastMessage(ServerPlayerEntity player, Text message) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            throw new IllegalStateException("尝试在客户端广播聊天消息");
        }
        PlayerManager playerManager = server.getPlayerManager();
        broadcastMessage(playerManager, message);
    }

    /**
     * 广播一条带有特殊样式的文本消息
     *
     * @param source  通过这个服务器命令源对象获取玩家管理器对象，然后通过玩家管理器对象发送消息，source不是消息的发送者
     * @param message 要广播消息的内容
     * @deprecated 使用服务器命令源作为参数有误导性
     */
    @Deprecated(forRemoval = true)
    public static void broadcastMessage(ServerCommandSource source, Text message) {
        PlayerManager playerManager = source.getServer().getPlayerManager();
        broadcastMessage(playerManager, message);
    }

    public static void broadcastMessage(MinecraftServer server, Text message) {
        broadcastMessage(server.getPlayerManager(), message);
    }

    /**
     * 广播一条带有特殊样式的文本消息
     *
     * @param playerManager 通过这个玩家管理器对象发送消息
     * @param message       要广播消息的内容
     */
    public static void broadcastMessage(PlayerManager playerManager, Text message) {
        playerManager.broadcast(message, false);
    }

    /**
     * 让一个玩家发送带有特殊样式的文本，文本内容仅对消息发送者可见
     *
     * @param player  要发送文本消息的玩家
     * @param message 发送文本消息的内容
     */
    public static void sendMessage(ServerPlayerEntity player, Text message) {
        player.sendMessage(message);
    }

    /**
     * 让一个玩家发送带有特殊样式的文本，文本内容仅对消息发送者可见
     *
     * @param source  要发送文本消息的命令源
     * @param message 发送文本消息的内容
     */
    public static void sendMessage(ServerCommandSource source, Text message) {
        source.sendMessage(message);
    }

    /**
     * 发送一条可以被翻译的消息做为命令的执行反馈，消息内容仅消息发送者可见
     */
    public static void sendMessage(CommandContext<ServerCommandSource> context, String key, Object... obj) {
        MessageUtils.sendMessage(context.getSource(), key, obj);
    }

    public static void sendMessage(ServerCommandSource source, String key, Object... obj) {
        MessageUtils.sendMessage(source, TextUtils.translate(key, obj));
    }

    /**
     * 发送一条红色的可以被翻译的消息做为命令的执行反馈，消息内容仅消息发送者可见
     */
    public static void sendErrorMessage(CommandContext<ServerCommandSource> context, String key, Object... obj) {
        MessageUtils.sendErrorMessage(context.getSource(), key, obj);
    }

    public static void sendErrorMessage(ServerCommandSource source, String key, Object... obj) {
        MessageUtils.sendMessage(source, TextUtils.setColor(TextUtils.translate(key, obj), Formatting.RED));
    }

    public static void sendErrorMessage(ServerCommandSource source, Text message) {
        MessageUtils.sendMessage(source, TextUtils.setColor(message.copy(), Formatting.RED));
    }

    /**
     * <br>发送一条红色的可以被翻译的消息做为命令的执行反馈，消息内容仅消息发送者可见<br/>
     * 鼠标悬停时可以显示异常信息
     *
     * @param source 消息的发送者
     * @param e      引发错误的异常对象
     * @param key    消息的翻译键
     * @param obj    消息中替代占位符的内容
     */
    public static void sendErrorMessage(ServerCommandSource source, Throwable e, String key, Object... obj) {
        // TODO 改为异常名+异常信息
        String error = Objects.requireNonNullElse(e.getMessage(), e.getClass().getSimpleName());
        MutableText message = TextUtils.setColor(TextUtils.translate(key, obj), Formatting.RED);
        MessageUtils.sendMessage(source, TextUtils.hoverText(message, TextUtils.createText(error)));
    }

    /**
     * 让一个玩家发送带有特殊样式的文本，文本会显示在屏幕中下方的HUD上，文本内容仅对消息发送者可见
     *
     * @param player  要发送文本消息的玩家
     * @param message 发送文本消息的内容
     */
    public static void sendMessageToHud(PlayerEntity player, Text message) {
        player.sendMessage(message, true);
    }

    /**
     * 发送多条带有特殊样式的消息，每一条消息单独占一行，消息内容仅发送者可见
     *
     * @param source 消息的发送者，消息内容仅发送者可见
     * @param list   存储所有要发送的消息的集合
     */
    public static void sendListMessage(ServerCommandSource source, ArrayList<? extends Text> list) {
        for (Text message : list) {
            sendMessage(source, message);
        }
    }
}
