package boat.carpetorgaddition.client.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;

import java.util.HashMap;
import java.util.List;

public class ClientCommandRegister {
    private static final HashMap<Class<? extends AbstractClientCommand>, AbstractClientCommand> commands = new HashMap<>();

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register(ClientCommandRegister::register);
    }

    private static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext access) {
        // 字典命令
        register(new DictionaryCommand(dispatcher, access));
        // 高亮路径点命令
        register(new HighlightCommand(dispatcher, access));
        register(new ClientFinderCommand(dispatcher, access));
        register(new FinderByNameCommand(dispatcher, access));
    }

    private static <T extends AbstractClientCommand> void register(T command) {
        command.register();
        Class<? extends AbstractClientCommand> clazz = command.getClass();
        commands.put(clazz, command);
    }

    public static <T extends AbstractClientCommand> T getCommandInstance(Class<T> clazz) {
        return clazz.cast(commands.get(clazz));
    }

    @SuppressWarnings("unused")
    public static List<AbstractClientCommand> listCommands() {
        return commands.values().stream().toList();
    }
}
