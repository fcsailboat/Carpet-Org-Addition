package boat.carpetorgaddition.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;

import java.util.HashMap;

public class CommandRegister {
    private static final HashMap<Class<? extends AbstractServerCommand>, AbstractServerCommand> commands = new HashMap<>();

    // 注册Carpet命令
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext access) {
        // 物品分身命令
        register(new ItemShadowingCommand(dispatcher, access));
        // 苦力怕音效命令
        register(new CreeperCommand(dispatcher, access));
        // 经验转移命令
        register(new XpTransferCommand(dispatcher, access));
        // 生存旁观切换命令
        register(new SpectatorCommand(dispatcher, access));
        // 查找器命令
        register(new FinderCommand(dispatcher, access));
        // 自杀命令
        register(new KillMeCommand(dispatcher, access));
        // 路径点管理器命令
        register(new LocationsCommand(dispatcher, access));
        // 绘制粒子线命令
        // ParticleLineCommand.register(dispatcher);
        // 假玩家动作命令
        register(new PlayerActionCommand(dispatcher, access));
        // 规则搜索命令
        register(new RuleSearchCommand(dispatcher, access));
        // 玩家管理器命令
        register(new PlayerManagerCommand(dispatcher, access));
        // 导航器命令
        register(new NavigatorCommand(dispatcher, access));
        // 快递命令
        register(new MailCommand(dispatcher, access));
        register(new OrangeCommand(dispatcher, access));
        register(new RuntimeCommand(dispatcher, access));
    }

    private static <T extends AbstractServerCommand> void register(T command) {
        if (command.shouldRegister()) {
            command.register();
            commands.put(command.getClass(), command);
        }
    }

    public static <T extends AbstractServerCommand> T getCommandInstance(Class<T> clazz) {
        return clazz.cast(commands.get(clazz));
    }
}
