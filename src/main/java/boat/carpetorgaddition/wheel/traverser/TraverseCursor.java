package boat.carpetorgaddition.wheel.traverser;

import org.jspecify.annotations.NonNull;

public interface TraverseCursor<T> {
    Step advance(long timeSlice);

    @NonNull
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
