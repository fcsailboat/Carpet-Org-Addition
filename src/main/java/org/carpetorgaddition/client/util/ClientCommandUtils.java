package org.carpetorgaddition.client.util;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.carpetorgaddition.CarpetOrgAddition;

public class ClientCommandUtils {
    private ClientCommandUtils() {
    }

    /**
     * 让客户端玩家向服务器发送一条命令
     *
     * @param command 命令的内容，不建议以“/”开头
     */
    public static void sendCommand(String command) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            CarpetOrgAddition.LOGGER.error("尝试在游戏外发送命令");
            return;
        }
        // 发送命令，发送前移除命令的斜杠
        player.networkHandler.sendCommand(command.startsWith("/") ? command.substring(1) : command);
    }

    /**
     * 读取一个单词，如果没有被引号包裹，读取到末尾或下一个空格，
     * 如果被引号包裹，读取到对应的引号
     *
     * @return 读取的字符串
     */
    public static String readWord(StringReader reader) throws CommandSyntaxException {
        // 有引号包裹
        if (StringReader.isQuotedStringStart(reader.peek())) {
            return reader.readString();
        }
        // 没有引号包裹
        int cursor = reader.getCursor();
        String remaining = reader.getRemaining();
        int index = remaining.indexOf(' ');
        String result;
        if (index == -1) {
            // 读取到末尾
            result = remaining;
        } else {
            // 读取到下一个空格
            result = remaining.substring(0, index);
            reader.skip();
        }
        // 重新设置光标位置
        reader.setCursor(cursor + result.length());
        return result;
    }
}
