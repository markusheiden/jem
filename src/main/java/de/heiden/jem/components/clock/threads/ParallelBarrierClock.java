package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.Tick;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import static de.heiden.jem.components.clock.ManualAbort.abortIfInterrupted;
import static java.lang.Thread.onSpinWait;
import static java.util.concurrent.locks.LockSupport.unpark;

/**
 * Clock implemented with synchronization, executing component threads in parallel.
 * All component threads wait at a barrier for the tick to end.
 * The last component thread reaching the barrier starts the next tick.
 */
public final class ParallelBarrierClock extends AbstractSynchronizedClock {
    /**
     * Number of component threads.
     */
    private int parties;

    /**
     * Number of component threads that reached the barrier in the current tick.
     */
    private final AtomicInteger arrived = new AtomicInteger(0);

    /**
     * Generation of the barrier. Incremented each time the barrier opens.
     * Package visible to avoid synthetic accessors.
     */
    volatile int generation = 0;

    /**
     * Number of component threads that are parked at the barrier or are about to park.
     */
    private final AtomicInteger parked = new AtomicInteger(0);

    /**
     * Component threads.
     */
    private Thread[] threads;

    /**
     * Start threads of all components.
     */
    @Override
    protected void doSynchronizedInit() {
        logger.debug("starting components");

        // Create threads.
        var components = clockedComponents();
        parties = components.length;
        Tick tick = this::await;
        for (var component : components) {
            component.setTick(tick);
        }
        var threads = new Thread[parties];
        for (var i = 0; i < parties; i++) {
            var component = components[i];
            threads[i] = createDaemonThread(component.getName(), () -> executeComponent(component, tick));
        }
        this.threads = threads;
        for (var thread : threads) {
            thread.start();
        }
    }

    /**
     * Wait at the barrier until all component threads reached it.
     * First busy waits, because parking and unparking a thread is expensive.
     */
    private void await() {
        // The generation can't change until this thread reached the barrier.
        var generation = this.generation;
        if (arrived.incrementAndGet() == parties) {
            // All other component threads are waiting at the barrier -> Start the next tick.
            startTick();
            // Open the barrier.
            arrived.set(0);
            this.generation = generation + 1;
            // Parking threads first announce themselves and then check the generation,
            // so here either the announcement is visible or they see the new generation.
            if (parked.get() != 0) {
                var current = Thread.currentThread();
                for (var thread : threads) {
                    if (thread != current) {
                        unpark(thread);
                    }
                }
            }
            return;
        }

        for (var i = spinLimit(); i > 0; i--) {
            if (this.generation != generation) {
                return;
            }
            onSpinWait();
        }
        parked.incrementAndGet();
        try {
            while (this.generation == generation) {
                // There is no problem, if this thread is not parked when it gets unparked:
                // In this case this park will not block, see LockSupport.unpark() javadoc.
                LockSupport.park(this);
                // park() does not block anymore, if the thread has been interrupted.
                abortIfInterrupted();
            }
        } finally {
            parked.decrementAndGet();
        }
    }
}
