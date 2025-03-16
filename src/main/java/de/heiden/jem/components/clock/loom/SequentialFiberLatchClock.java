package de.heiden.jem.components.clock.loom;

import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;

import de.heiden.jem.components.clock.AbstractSimpleClock;

/**
 * Clock executing {@link Thread fibers} of project loom on an {@link Executor}.
 */
public final class SequentialFiberLatchClock extends AbstractSimpleClock {
    @Override
    protected void doRun() {
        var semaphores = createVirtualThreads(true);
        //noinspection InfiniteLoopStatement
        semaphores.first.release();
    }

    @Override
    protected void doRun(int ticks) {
        assert ticks >= 0 : "Precondition: ticks >= 0";

        var semaphores = createVirtualThreads(false);
        var firstSemaphore = semaphores.first;
        var lastSemaphore = semaphores.last;
        for (final long stop = getTick() + ticks; getTick() < stop; ) {
            try {
                firstSemaphore.release();
                lastSemaphore.acquire();
            } catch (Exception e) {
                throw new IllegalStateException("May not happen.", e);
            }
        }
    }

    /**
     * Create a virtual thread for each component.
     */
    private Semaphores createVirtualThreads(boolean endless) {
        var components = clockedComponents();

        var semaphores = new Semaphore[components.length + 1];
        for (int i = 0; i < semaphores.length; i++) {
            semaphores[i] = new Semaphore(0);
        }
        if (endless) {
            semaphores[semaphores.length - 1] = semaphores[0];
        }

        for (int i = 0; i < components.length; i++) {
            var component = components[i];
            var semaphore = semaphores[i];
            var next = semaphores[i + 1];
            if (i == 0) {
                component.setTick(() -> {
                    try {
                        startTick();
                        next.release();
                        semaphore.acquire();
                    } catch (InterruptedException e) {
                        throw new IllegalStateException("May not happen.", e);
                    }
                });
            } else {
                component.setTick(() -> {
                    try {
                        next.release();
                        semaphore.acquire();
                    } catch (InterruptedException e) {
                        throw new IllegalStateException("May not happen.", e);
                    }
                });
            }
            Thread.ofVirtual().start(() -> {
                try {
                    semaphore.acquire();
                    component.run();
                } catch (InterruptedException e) {
                    throw new IllegalStateException("May not happen.", e);
                }
            });
        }

        return new Semaphores(semaphores[0], semaphores[semaphores.length - 1]);
    }

    private record Semaphores(Semaphore first, Semaphore last) {}
}
