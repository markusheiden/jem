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
            // Execute next component and wait for the next tick. The last component hands over to the clock.
            var next = semaphores[i + 1];
            component.setTick(() -> executeNext(next, semaphore));
            fibers[i] = Thread.ofVirtual().name(component.getName()).start(() -> {
                // Wait for the first tick.
                acquire(semaphore);
                component.run();
            });
        }
    }

    @Override
    protected void doRun() {
        var first = semaphores[0];
        var last = semaphores[semaphores.length - 1];
        //noinspection InfiniteLoopStatement
        while (true) {
            startTick();
            // Execute first component and wait for the last component to finish the tick.
            executeNext(first, last);
        }
    }

    @Override
    protected void doRun(int ticks) {
        assert ticks >= 0 : "Precondition: ticks >= 0";

        var first = semaphores[0];
        var last = semaphores[semaphores.length - 1];
        for (long stop = getTick() + ticks; getTick() < stop; ) {
            startTick();
            // Execute first component and wait for the last component to finish the tick.
            executeNext(first, last);
        }
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
