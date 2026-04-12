package boat.carpetorgaddition.client.command;

import boat.carpetorgaddition.command.AbstractCommand;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;

@SuppressWarnings("unused")
public abstract class AbstractClientCommand extends AbstractCommand {
    protected final CommandDispatcher<FabricClientCommandSource> dispatcher;
    protected final CommandBuildContext access;

    public AbstractClientCommand(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext access) {
        this.dispatcher = dispatcher;
        this.access = access;
    }

    @Override
    protected final String getEnvironment() {
        return "Client";
    }
}
