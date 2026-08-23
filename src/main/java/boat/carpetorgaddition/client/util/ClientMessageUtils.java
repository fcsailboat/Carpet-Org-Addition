package boat.carpetorgaddition.client.util;

import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public class ClientMessageUtils {
    private ClientMessageUtils() {
    }

    public static void sendEmptyMessage() {
        sendMessage(TextBuilder.empty());
    }

    /**
     * 向客户端玩家发送一条聊天消息
     */
    public static void sendMessage(Component message) {
        LocalPlayer player = ClientUtils.getPlayer();
        player.sendSystemMessage(message);
    }

    /**
     * 向客户端玩家发送一条红色的聊天消息
     */
    public static void sendErrorMessage(Component message) {
        sendMessage(TextBuilder.of(message).setColor(ChatFormatting.RED).build());
    }

    public static void sendErrorMessage(Component component, Throwable e) {
        String error = CommandUtils.getExceptionString(e);
        TextBuilder builder = TextBuilder.of(component);
        builder.setHover(error);
        builder.setColor(ChatFormatting.RED);
        sendErrorMessage(builder.build());
    }
}
