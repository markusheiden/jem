package de.heiden.jem.components.clock.loom;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import de.heiden.jem.components.clock.AbstractSimpleClock;
import de.heiden.jem.components.clock.ClockedComponent;

/**
 * Clock executing {@link Thread fibers} of project loom on an {@link Executor}.
 */
public final class SequentialFiberExecutorClock extends AbstractSimpleClock {
    /**
     * The one and only thread to execute the fibers.
     */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void doRun() {
        var fibers = createFibers();
        while (true) {
            startTick();
            for (var thread : fibers) {
                thread.run();
            }
        }
    }

    @Override
    protected void doRun(int ticks) {
        assert ticks >= 0 : "Precondition: ticks >= 0";

        var fibers = createFibers();
        for (final long stop = getTick() + ticks; getTick() < stop; ) {
            startTick();
            for (var thread : fibers) {
                thread.run();
            }
        }
    }

    /**
     * Create fibers.
     */
    private Thread[] createFibers() {
        var components = _componentMap.values().toArray(ClockedComponent[]::new);

        var fibers = new Thread[components.length];
        for (int i = 0; i < components.length; i++) {
            var component = components[i];
            component.setTick(Thread::yield);
            fibers[i] = Thread.ofVirtual()
                    .scheduler(executor)
                    .uncaughtExceptionHandler(this::uncaughtException)
                    .unstarted(component::run);
        }

        return fibers;
    }

    @Override
    protected void doClose() {
        executor.shutdown();
    }

    /**
     * Ignore {@link RejectedExecutionException}s which are caused by the executor being shutdown.
     * Rethrows all other exceptions.
     */
    private void uncaughtException(Thread t, Throwable e) {
        if (e instanceof Error error) {
            throw error;
        }

        if (e instanceof RejectedExecutionException) {
            // Thread pool has been closed. Abort execution.
            return;
        }

        if (e instanceof RuntimeException runtime) {
            throw runtime;
        }

        throw new RuntimeException(e);
    }
}
