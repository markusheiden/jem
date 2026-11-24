package de.heiden.jem.components.clock.loom;

import de.heiden.jem.components.clock.ManualAbort;

import java.util.concurrent.SynchronousQueue;

/**
 * Clock using {@link Thread fibers} from project loom.
 * The thread running the clock and the fibers of the components hand over execution via {@link SynchronousQueue}s.
 */
public final class SequentialFiberQueueClock extends AbstractFiberClock {
    /**
     * Token handed over to the next thread to execute.
     */
    private static final Object TOKEN = new Object();

    /**
     * Queues of the components followed by the queue of the thread running the clock.
     */
    @SuppressWarnings("NullAway.Init")
    private SynchronousQueue<Object>[] queues;

    @Override
    @SuppressWarnings("unchecked")
    protected void init(int numThreads) {
        var queues = new SynchronousQueue[numThreads];
        for (var i = 0; i < numThreads; i++) {
            queues[i] = new SynchronousQueue<>();
        }
        this.queues = queues;
    }

    @Override
    protected void resume(int index) {
        try {
            // Blocks until the thread takes the token.
            queues[index].put(TOKEN);
        } catch (InterruptedException e) {
            throw new ManualAbort();
        }
    }

    @Override
    protected void await(int index) {
        try {
            queues[index].take();
        } catch (InterruptedException e) {
            throw new ManualAbort();
        }
    }
}
