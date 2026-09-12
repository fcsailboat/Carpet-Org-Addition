package boat.carpetorgaddition.client.command;

import boat.carpetorgaddition.client.command.argument.ClientObjectArgumentType;
import boat.carpetorgaddition.client.util.ClientMessageUtils;
import boat.carpetorgaddition.wheel.common.CommonTexts;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;

public class DictionaryCommand extends AbstractClientCommand {
    public static final String DEFAULT_COMMAND_NAME = "dictionary";
    public static final LocalizationKey KEY = LocalizationKeys.COMMAND.then("dictionary");
    public static final LocalizationKey MULTIPLE = KEY.then("multiple");

    public DictionaryCommand(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        LiteralArgumentBuilder<FabricClientCommandSource> builder = ClientCommands.literal(name);
        // 注册每一项子命令
        for (Map.Entry<String, ClientObjectArgumentType<?>> entry : ClientObjectArgumentType.getDictionaryTypes().entrySet()) {
            builder.then(ClientCommands.literal(entry.getKey())
                    .then(ClientCommands.argument(entry.getKey(), entry.getValue())
                            .executes(context -> getId(context, entry.getKey(), entry.getValue()))));
        }
        this.dispatcher.register(builder);
    }

    // 获取对象id
    private int getId(CommandContext<FabricClientCommandSource> context, String name, ClientObjectArgumentType<?> type) {
        List<?> list = ClientObjectArgumentType.getType(context, name);
        // list集合至少有一个元素，不与任何对象匹配的字符串在解析命令时不会成功
        if (list.size() == 1) {
            // 字符串只对应一个对象
            Object obj = list.getFirst();
            // 获取对象id
            String id = type.getObjectIdAsString(obj);
            this.sendFeedback(type.getObjectName(obj), id);
        } else {
            // 字符串对应多个对象
            sendFeedback(list.size());
            for (Object obj : list) {
                this.sendFeedback(type.getObjectIdAsString(obj));
            }
        }
        return list.size();
    }

    // 发送命令反馈
    private void sendFeedback(Component text, String id) {
        ClientMessageUtils.sendMessage(KEY.then("single").translate(text, canCopyId(id)));
    }

    private void sendFeedback(int count) {
        ClientMessageUtils.sendMessage(MULTIPLE.then("head").translate(count));
    }

    private void sendFeedback(String id) {
        ClientMessageUtils.sendMessage(MULTIPLE.then("each").translate(canCopyId(id)));
    }

    // 将字符串id转换成可以单击复制的形式
    @NonNull
    private Component canCopyId(String id) {
        return TextBuilder.of(id)
                .setCopyToClipboard(id)
                .setHover(CommonTexts.COPY_CLICK)
                .setColor(ChatFormatting.GREEN)
                .build();
    }

    @Override
    public String getDefaultName() {
        return DEFAULT_COMMAND_NAME;
    }
}
