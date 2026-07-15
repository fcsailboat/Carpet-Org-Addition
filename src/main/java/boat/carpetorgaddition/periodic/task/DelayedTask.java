package boat.carpetorgaddition.periodic.task;

import net.minecraft.commands.CommandSourceStack;

public class DelayedTask extends ServerTask {
    private int time;
    private final Runnable task;
    private boolean complete = false;

    public DelayedTask(CommandSourceStack source, int time, Runnable task) {
        if (time <= 0) {
            throw new IllegalArgumentException();
        }
        super(source);
        this.time = time;
        this.task = task;
    }

    @Override
    protected void tick() {
        this.time--;
        if (this.time <= 0) {
            this.task.run();
            this.complete = true;
        }
    }

    @Override
    protected boolean stopped() {
        return this.complete;
    }

    @Override
    public Object getIdentityKey() {
        return this;
    }
}
