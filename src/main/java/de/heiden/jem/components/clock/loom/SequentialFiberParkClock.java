package de.heiden.jem.components.clock.loom;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.locks.LockSupport;

import de.heiden.jem.components.clock.AbstractSimpleClock;
import de.heiden.jem.components.clock.ClockedComponent;

/**
 * Clock using {@link Thread fibers} from project loom.
 */
public final class SequentialFiberParkClock extends AbstractSimpleClock {
    /**
     * The one and only thread to execute the fibers.
     */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Currently executed component.
     */
    private int state = -1;

    @Override
    protected void doRun() {
        createFibers(-1);
    }

    @Override
    protected void doRun(int ticks) {
        assert ticks >= 0 : "Precondition: ticks >= 0";

        createFibers(ticks);
    }

    /**
     * Create fibers.
     */
    private void createFibers(final int ticks) {
        var components = _componentMap.values().toArray(new ClockedComponent[0]);
        var numComponents = components.length;

        var fibers = new Thread[numComponents + 1];
        for (int i = 0; i < numComponents; i++) {
            var component = components[i];
            fibers[i] = buildVirtualThread(component::run);
        }

        Thread starterFiber;
        if (ticks < 0) {
            starterFiber = buildVirtualThread(() -> {
                //noinspection InfiniteLoopStatement
                while (true) {
                    startTick();
                    // Execute first component and wait for last tick.
                    executeNextComponent(0, fibers[0], numComponents);
                }
            });
        } else {
            starterFiber = buildVirtualThread(() -> {
                for (final long stop = getTick() + ticks; getTick() < stop; ) {
                    startTick();
                    // Execute first component and wait for last tick.
                    executeNextComponent(0, fibers[0], numComponents);
                }
                executor.shutdownNow();
            });
        }
        fibers[numComponents] = starterFiber;

        for (int i = 0; i < numComponents; i++) {
            var component = components[i];
            // Execute next component thread and wait for next tick.
            var state = i;
            var nextState = i + 1;
            var nextFiber = fibers[nextState];
            component.setTick(() -> executeNextComponent(nextState, nextFiber, state));
        }

        for (var fiber : fibers) {
            fiber.start();
        }
    }

    private Thread buildVirtualThread(Runnable task) {
        return Thread.ofVirtual()
                .scheduler(executor)
                .uncaughtExceptionHandler(this::uncaughtException)
                .unstarted(task);
    }

    /**
     * Execute next component and wait for this component to execute.
     */
    private void executeNextComponent(final int nextState, final Thread nextFiber, final int state) {
        this.state = nextState;
        LockSupport.unpark(nextFiber);
        LockSupport.park();
        while (this.state != state) {
            Thread.yield();
        }
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