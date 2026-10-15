package boat.carpetorgaddition.wheel.traverser;

public interface TraverseCursor<T> {
    Step advance(long timeSlice);

    T take();

    default long getProcessedCount() {
        throw new UnsupportedOperationException();
    }

    enum Step {
        ELEMENT,
        PAUSED,
        DONE
    }
}
