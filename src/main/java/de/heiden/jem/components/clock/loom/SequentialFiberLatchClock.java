package de.heiden.jem.components.clock.loom;

import de.heiden.jem.components.clock.ManualAbort;

import java.util.concurrent.Semaphore;

/**
 * Clock using {@link Thread fibers} from project loom.
 * The thread running the clock and the fibers of the components hand over execution via semaphores.
 */
public final class SequentialFiberLatchClock extends AbstractFiberClock {
    /**
     * Semaphores of the components followed by the semaphore of the thread running the clock.
     */
    @SuppressWarnings("NullAway.Init")
    private Semaphore[] semaphores;

    @Override
    protected void init(int numThreads) {
        var semaphores = new Semaphore[numThreads];
        for (var i = 0; i < numThreads; i++) {
            semaphores[i] = new Semaphore(0);
        }
        this.semaphores = semaphores;
    }

    @Override
    protected void resume(int index) {
        semaphores[index].release();
    }

    @Override
    protected void await(int index) {
        try {
            semaphores[index].acquire();
        } catch (InterruptedException e) {
            throw new ManualAbort();
        }
    }
}
