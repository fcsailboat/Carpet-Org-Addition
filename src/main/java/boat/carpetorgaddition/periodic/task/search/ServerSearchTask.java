package boat.carpetorgaddition.periodic.task.search;

import boat.carpetorgaddition.command.FinderCommand;
import boat.carpetorgaddition.exception.TaskExecutionException;
import boat.carpetorgaddition.periodic.task.ServerTask;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.wheel.provider.CommandProvider;
import boat.carpetorgaddition.wheel.provider.TextProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public abstract class ServerSearchTask extends ServerTask {
    private static final Object IDENTITY_KEY = new Object();
    protected final ServerPlayer player;

    public ServerSearchTask(CommandSourceStack source, ServerPlayer player) {
        super(source);
        this.player = player;
    }

    private boolean notice = false;

    protected void noticeCancelled() {
        if (this.notice) {
            return;
        }
        Component run = TextProvider.clickRun(CommandProvider.finderStop());
        Component message = FinderCommand.KEY.then("waiting_to_be_completed").translate(run);
        MessageUtils.sendMessage(this.source, message);
        this.notice = true;
    }

    public abstract void cancel();

    protected abstract boolean isCancelled();

    protected void checkCancelled() {
        if (this.isCancelled()) {
            throw new TaskExecutionException(() -> MessageUtils.sendMessage(this.source, FinderCommand.KEY.then("cancelled").translate()));
        }
    }

    public static Object createIdentityKey(ServerPlayer player) {
        return Map.entry(IDENTITY_KEY, player);
    }

    @Override
    public final Object getIdentityKey() {
        return createIdentityKey(this.player);
    }
}
