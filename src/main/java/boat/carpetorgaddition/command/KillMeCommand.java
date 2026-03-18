package boat.carpetorgaddition.command;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

// 自杀命令
public class KillMeCommand extends AbstractServerCommand {
    public static final LocalizationKey KEY = LocalizationKeys.COMMAND.then("killMe");

    public KillMeCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        this.dispatcher.register(Commands.literal(name)
                .requires(CommandUtils.canUseCommand(CarpetOrgAdditionSettings.COMMAND_KILL_ME))
                .executes(this::killMe));
    }

    // 玩家自杀
    private int killMe(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        // 广播自杀消息
        ScopedValue.where(CarpetOrgAdditionSettings.COMMITTING_SUICIDE, true).run(() -> player.kill(player.level()));
        return 1;
    }

    @Override
    public String getDefaultName() {
        return "killMe";
    }
}
