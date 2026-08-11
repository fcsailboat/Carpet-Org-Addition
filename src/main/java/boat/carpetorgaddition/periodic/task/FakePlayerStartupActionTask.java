package boat.carpetorgaddition.periodic.task;

import boat.carpetorgaddition.periodic.fakeplayer.FakePlayerStartupAction;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.commands.CommandSourceStack;

import java.util.Map;

public class FakePlayerStartupActionTask extends ServerTask {
    private final EntityPlayerMPFake fakePlayer;
    private final FakePlayerStartupAction action;
    private int delay;

    public FakePlayerStartupActionTask(CommandSourceStack source, EntityPlayerMPFake fakePlayer, FakePlayerStartupAction action, int delay) {
        super(source);
        this.fakePlayer = fakePlayer;
        this.action = action;
        this.delay = delay;
    }

    @Override
    protected void tick() {
        this.delay--;
        if (this.delay == 0) {
            this.action.accept(this.fakePlayer);
        }
    }

    @Override
    protected boolean stopped() {
        return this.delay < 0;
    }

    @Override
    public Object getIdentityKey() {
        return Map.entry(this.fakePlayer, this.action);
    }
}
