package de.heiden.jem.components.clock.loom;

/**
 * Clock using {@link Thread fibers} from project loom.
 * The thread running the clock and the fibers of the components hand over execution via a state and yield.
 * Threads waiting for long park instead of yielding.
 */
public final class SequentialFiberYieldClock extends AbstractFiberClock {
    /**
     * Handover of execution between the fibers and the thread running the clock.
     */
    private YieldHandover handover;

    @Override
    protected void init(int numThreads) {
        // The thread running the clock executes first.
        handover = new YieldHandover(numThreads, numThreads - 1, numThreads - 1);
    }

    @Override
    protected void resume(int index) {
        handover.resume(index);
    }

    @Override
    protected void await(int index) {
        handover.await(index);
    }
}
