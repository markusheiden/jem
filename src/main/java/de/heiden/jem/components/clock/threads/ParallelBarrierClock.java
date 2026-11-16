package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.ManualAbort;
import de.heiden.jem.components.clock.Tick;

import java.util.concurrent.atomic.AtomicInteger;

import static de.heiden.jem.components.clock.ManualAbort.abortIfInterrupted;
import static java.lang.Thread.onSpinWait;

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
     * Monitor for component threads blocked at the barrier.
     */
    private final Object barrier = new Object();

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
        for (var component : components) {
            createStartedDaemonThread(component.getName(), () -> executeComponent(component, tick));
        }
    }

    /**
     * Wait at the barrier until all component threads reached it.
     * First busy waits, because blocking and notifying a thread is expensive.
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
            synchronized (barrier) {
                barrier.notifyAll();
            }
            return;
        }

        for (var i = spinLimit(); i > 0; i--) {
            if (this.generation != generation) {
                return;
            }
            onSpinWait();
        }
        abortIfInterrupted();
        synchronized (barrier) {
            try {
                while (this.generation == generation) {
                    barrier.wait();
                }
            } catch (InterruptedException e) {
                throw new ManualAbort();
            }
        }
    }
}
