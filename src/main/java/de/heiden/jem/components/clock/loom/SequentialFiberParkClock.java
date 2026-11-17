package de.heiden.jem.components.clock.loom;

import java.util.concurrent.locks.LockSupport;

import static de.heiden.jem.components.clock.ManualAbort.abortIfInterrupted;

/**
 * Clock using {@link Thread fibers} from project loom.
 * The thread running the clock and the fibers of the components hand over execution via park / unpark.
 */
public final class SequentialFiberParkClock extends AbstractFiberClock {
    /**
     * Index of the thread that is allowed to execute.
     */
    private volatile int current;

    @Override
    protected void init(int numThreads) {
        // The thread running the clock executes first.
        current = numThreads - 1;
    }

    @Override
    protected void resume(int index) {
        current = index;
        LockSupport.unpark(thread(index));
    }

    @Override
    protected void await(int index) {
        while (current != index) {
            // There is no problem, if this thread is not parked when the previous threads unparks it:
            // In this case this park will not block, see LockSupport.unpark() javadoc.
            LockSupport.park(this);
            // park() does not block anymore, if the thread has been interrupted.
            abortIfInterrupted();
        }
    }
}
