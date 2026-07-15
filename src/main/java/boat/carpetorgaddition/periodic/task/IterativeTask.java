package boat.carpetorgaddition.periodic.task;

import net.minecraft.commands.CommandSourceStack;

import java.util.Iterator;

public class IterativeTask extends ServerTask {
    private final Iterator<Runnable> iterator;
    private final long sliceTime;
    private final long timeoutTime;

    public IterativeTask(CommandSourceStack source, Iterable<Runnable> runs, long sliceTime, long timeoutTime) {
        super(source);
        this.iterator = runs.iterator();
        this.sliceTime = sliceTime;
        this.timeoutTime = timeoutTime;
    }

    @Override
    protected void tick() {
        while (this.iterator.hasNext() && this.isTimeRemaining()) {
            this.checkTimeout();
            this.iterator.next().run();
        }
    }

    @Override
    protected long getMaxExecutionTime() {
        return this.timeoutTime;
    }

    @Override
    protected long getMaxTimeSlice() {
        return this.sliceTime;
    }

    @Override
    protected boolean stopped() {
        return !this.iterator.hasNext();
    }

    @Override
    public Object getIdentityKey() {
        return this;
    }
}
