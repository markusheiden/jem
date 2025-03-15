package de.heiden.jem.components.clock.loom;

import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;

import de.heiden.jem.components.clock.AbstractSimpleClock;
import de.heiden.jem.components.clock.ClockedComponent;

/**
 * Clock executing {@link Thread fibers} of project loom on an {@link Executor}.
 */
public final class SequentialFiberLatchClock extends AbstractSimpleClock {
    @Override
    protected void doRun() {
        var semaphores = createVirtualThreads(true);
        var firstSemaphore = semaphores[0];
        var lastSemaphore = semaphores[1];
        //noinspection InfiniteLoopStatement
        firstSemaphore.release();
    }

    @Override
    protected void doRun(int ticks) {
        assert ticks >= 0 : "Precondition: ticks >= 0";

        var semaphores = createVirtualThreads(false);
        var firstSemaphore = semaphores[0];
        var lastSemaphore = semaphores[1];
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
    private Semaphore[] createVirtualThreads(boolean endless) {
        var components = _componentMap.values().toArray(ClockedComponent[]::new);

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

        return new Semaphore[] { semaphores[0], semaphores[semaphores.length - 1] };
    }
}
