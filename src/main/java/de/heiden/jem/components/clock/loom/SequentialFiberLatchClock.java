package de.heiden.jem.components.clock.loom;

import de.heiden.jem.components.clock.AbstractSimpleClock;
import de.heiden.jem.components.clock.ManualAbort;

import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * Clock using {@link Thread fibers} from project loom.
 * The thread running the clock and the fibers of the components hand over execution via semaphores.
 */
public final class SequentialFiberLatchClock extends AbstractSimpleClock {
    /**
     * Fibers of the components.
     */
    private Thread[] fibers;

    /**
     * Semaphores of the components followed by the semaphore of the thread running the clock.
     */
    private Semaphore[] semaphores;

    /**
     * Last tick to execute.
     * Written by the clock thread before handing over execution to the fibers.
     */
    private long stop;

    @Override
    protected void doInit() {
        var components = clockedComponents();
        var numComponents = components.length;

        semaphores = new Semaphore[numComponents + 1];
        for (var i = 0; i < semaphores.length; i++) {
            semaphores[i] = new Semaphore(0);
        }

        fibers = new Thread[numComponents];
        for (var i = 0; i < numComponents; i++) {
            var component = components[i];
            var semaphore = semaphores[i];
            var next = semaphores[i + 1];
            if (i + 1 < numComponents) {
                // Execute next component and wait for the next tick.
                component.setTick(() -> executeNext(next, semaphore));
            } else {
                // The last component starts the next tick itself to avoid handing over to the clock for each tick.
                var first = semaphores[0];
                component.setTick(() -> {
                    if (getTick() < stop) {
                        startTick();
                        executeNext(first, semaphore);
                    } else {
                        executeNext(next, semaphore);
                    }
                });
            }
            fibers[i] = Thread.ofVirtual().name(component.getName()).start(() -> runUntilAbort(() -> {
                // Wait for the first tick.
                acquire(semaphore);
                component.run();
            }));
        }
    }

    @Override
    protected void doRun() {
        execute(Long.MAX_VALUE);
    }

    @Override
    protected void doRun(int ticks) {
        assert ticks >= 0 : "Precondition: ticks >= 0";

        execute(getTick() + ticks);
    }

    /**
     * Execute ticks until the given tick.
     * The fibers execute the ticks and hand over to the clock thread after the last tick.
     */
    private void execute(long stop) {
        if (getTick() >= stop) {
            return;
        }
        this.stop = stop;
        startTick();
        // Execute first component and wait for the last component to finish the last tick.
        executeNext(semaphores[0], semaphores[semaphores.length - 1]);
    }

    /**
     * Execute next thread and wait for the current thread to execute again.
     */
    private static void executeNext(Semaphore next, Semaphore semaphore) {
        next.release();
        acquire(semaphore);
    }

    /**
     * Acquire the semaphore.
     */
    private static void acquire(Semaphore semaphore) {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            throw new ManualAbort();
        }
    }

    @Override
    protected void doClose() {
        if (fibers != null) {
            terminate(List.of(fibers));
            fibers = null;
            semaphores = null;
        }
    }
}
