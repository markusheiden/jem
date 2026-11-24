package de.heiden.jem.components.clock.loom;

import de.heiden.jem.components.clock.AbstractSimpleClock;
import org.jspecify.annotations.Nullable;

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Base implementation for all clocks using {@link Thread fibers} from project loom.
 * The fibers of the components execute one after another, the implementations define how they hand over execution.
 * The fiber of the last component starts the next tick, so all components and events are executed by fibers.
 * The thread running the clock just waits for the fibers to finish the last tick.
 * <p>
 * All components and events are executed by the same thread, if the fibers of this JVM are limited to a
 * {@link FiberScheduler#useSingleCarrier() single carrier thread}. Otherwise, a warning is logged.
 */
public abstract class AbstractFiberClock extends AbstractSimpleClock {
    /**
     * Fibers of the components followed by the thread running the clock.
     */
    private Thread @Nullable [] threads;

    /**
     * Last tick to execute.
     * Written by the thread running the clock before handing over execution to the fibers.
     */
    private long stop;

    @Override
    protected final void doInit() {
        if (!FiberScheduler.isSingleCarrier()) {
            logger.warn("Fibers are not limited to a single carrier thread: Components are executed by multiple threads.");
        }

        var components = clockedComponents();
        var numComponents = components.length;
        var last = numComponents - 1;

        var threads = new Thread[numComponents + 1];
        for (var i = 0; i < last; i++) {
            var component = components[i];
            var index = i;
            var next = i + 1;
            // Execute next component and wait for the next tick.
            component.setTick(() -> {
                resume(next);
                await(index);
            });
            threads[i] = Thread.ofVirtual().name(component.getName()).unstarted(() -> runUntilAbort(() -> {
                // Wait for the first tick.
                await(index);
                component.run();
            }));
        }

        // The last component starts the ticks to avoid handing over to the thread running the clock for each tick.
        var component = components[last];
        component.setTick(() -> {
            if (getTick() >= stop) {
                // Execute the thread running the clock and wait for the next run.
                resume(numComponents);
                await(last);
            }
            startTickAndWait(last);
        });
        threads[last] = Thread.ofVirtual().name(component.getName()).unstarted(() -> runUntilAbort(() -> {
            // Wait for the first run.
            await(last);
            startTickAndWait(last);
            component.run();
        }));

        // Placeholder for the thread running the clock, which is set on each execution.
        threads[numComponents] = Thread.currentThread();

        this.threads = threads;
        init(numComponents + 1);
        for (var i = 0; i < numComponents; i++) {
            threads[i].start();
        }
    }

    /**
     * Start a new tick and wait for the last component to execute.
     */
    private void startTickAndWait(int last) {
        startTick();
        if (last != 0) {
            // Execute first component and wait for all other components.
            resume(0);
            await(last);
        }
    }

    @Override
    protected final void doRun() {
        execute(Long.MAX_VALUE);
    }

    @Override
    protected final void doRun(int ticks) {
        assert ticks >= 0 : "Precondition: ticks >= 0";

        execute(getTick() + ticks);
    }

    /**
     * Execute ticks until the given tick.
     * The fibers execute the ticks and hand over to the current thread after the last tick.
     */
    private void execute(long stop) {
        if (getTick() >= stop) {
            return;
        }
        var threads = requireNonNull(this.threads, "Clock is not started or already closed");
        var clock = threads.length - 1;
        threads[clock] = Thread.currentThread();
        this.stop = stop;
        // Execute the last component, which starts the ticks, and wait for it to finish the last tick.
        resume(clock - 1);
        await(clock);
    }

    /**
     * Init handing over execution.
     * Called before the fibers are started.
     *
     * @param numThreads
     *         Number of fibers plus one for the thread running the clock.
     */
    protected abstract void init(int numThreads);

    /**
     * Let the thread execute.
     *
     * @param index
     *         Index of the thread to execute. The thread running the clock has the highest index.
     */
    protected abstract void resume(int index);

    /**
     * Wait until the current thread is allowed to execute.
     *
     * @param index
     *         Index of the current thread.
     */
    protected abstract void await(int index);

    /**
     * Thread with the given index.
     * The thread running the clock is only valid while it runs the clock.
     */
    protected final Thread thread(int index) {
        return requireNonNull(threads, "Clock is not started or already closed")[index];
    }

    @Override
    protected void doClose() {
        var threads = this.threads;
        if (threads != null) {
            terminate(List.of(threads).subList(0, threads.length - 1));
            this.threads = null;
        }
    }
}
