package boat.carpetorgaddition.command;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.periodic.task.CreeperExplosionTask;
import boat.carpetorgaddition.periodic.task.ServerTaskManager;
import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.util.PlayerUtils;
import boat.carpetorgaddition.util.ServerUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class CreeperCommand extends AbstractServerCommand {
    public CreeperCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        this.dispatcher.register(Commands.literal(name)
                .requires(CommandUtils.canUseCommand(CarpetOrgAdditionSettings.commandCreeper))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(this::creeperExplosion)));
    }

    // 创建苦力怕并爆炸
    private int creeperExplosion(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer targetPlayer = CommandUtils.getArgumentPlayer(context);
        CommandSourceStack source = context.getSource();
        MinecraftServer server = ServerUtils.getServer(source);
        ServerTaskManager manager = ServerComponentCoordinator.getCoordinator(server).getServerTaskManager();
        // 添加苦力怕爆炸任务
        manager.addTask(new CreeperExplosionTask(source, targetPlayer));
        ServerPlayer sourcePlayer = source.getPlayer();
        if (sourcePlayer != null) {
            CarpetOrgAddition.LOGGER.info(
                    "{} triggered a creeper explosion around {}",
                    PlayerUtils.getName(sourcePlayer),
                    PlayerUtils.getName(targetPlayer)
            );
        }
        return 1;
    }

    @Override
    public String getDefaultName() {
        return "creeper";
    }
}
