package de.heiden.jem.components.clock.loom;

import java.util.concurrent.Executor;

import de.heiden.jem.components.clock.AbstractSimpleClock;

/**
 * Clock executing {@link Thread fibers} of project loom on an {@link Executor}.
 */
public final class SequentialFiberExecutorClock extends AbstractSimpleClock {
    @Override
    protected void doRun() {
        var virtualThreads = createVirtualThreads();
        //noinspection InfiniteLoopStatement
        while (true) {
            startTick();
            for (var thread : virtualThreads) {
                thread.run();
            }
        }
    }

    @Override
    protected void doRun(int ticks) {
        assert ticks >= 0 : "Precondition: ticks >= 0";

        var virtualThreads = createVirtualThreads();
        for (final long stop = getTick() + ticks; getTick() < stop; ) {
            startTick();
            for (var thread : virtualThreads) {
                thread.run();
            }
        }
    }

    /**
     * Create a virtual thread for each component.
     */
    private Thread[] createVirtualThreads() {
        var components = clockedComponents();

        var virtualThreads = new Thread[components.length];
        for (int i = 0; i < components.length; i++) {
            var component = components[i];
            component.setTick(Thread::yield);
            virtualThreads[i] = Thread.ofVirtual().start(() -> {
                Thread.yield();
                component.run();
            });
        }

        // Let the threads run into the first yield().
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // Ignore.
        }

        return virtualThreads;
    }
}
