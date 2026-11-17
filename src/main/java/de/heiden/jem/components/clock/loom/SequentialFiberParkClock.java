package de.heiden.jem.components.clock.loom;

import de.heiden.jem.components.clock.AbstractSimpleClock;

import java.util.List;
import java.util.concurrent.locks.LockSupport;

import static de.heiden.jem.components.clock.ManualAbort.abortIfInterrupted;

/**
 * Clock using {@link Thread fibers} from project loom.
 * The thread running the clock and the fibers of the components hand over execution via park / unpark.
 */
public final class SequentialFiberParkClock extends AbstractSimpleClock {
    /**
     * Fibers of the components.
     */
    private Thread[] fibers;

    /**
     * Thread running the clock.
     */
    private volatile Thread clockThread;

    /**
     * Thread that is allowed to execute.
     */
    private volatile Thread current;

    /**
     * Last tick to execute.
     * Written by the clock thread before handing over execution to the fibers.
     */
    private long stop;

    @Override
    protected void doInit() {
        var components = clockedComponents();
        var numComponents = components.length;

        fibers = new Thread[numComponents];
        for (var i = 0; i < numComponents; i++) {
            var component = components[i];
            fibers[i] = Thread.ofVirtual().name(component.getName()).unstarted(() -> runUntilAbort(() -> {
                // Wait for the first tick.
                waitForExecution();
                component.run();
            }));
        }

        for (var i = 0; i < numComponents - 1; i++) {
            // Execute next component and wait for the next tick.
            var next = fibers[i + 1];
            components[i].setTick(() -> executeNext(next));
        }
        // The last component starts the next tick itself to avoid handing over to the clock thread for each tick.
        var first = fibers[0];
        components[numComponents - 1].setTick(() -> {
            if (getTick() < stop) {
                startTick();
                executeNext(first);
            } else {
                executeNext(clockThread);
            }
        });

        for (var fiber : fibers) {
            fiber.start();
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
        clockThread = Thread.currentThread();
        this.stop = stop;
        startTick();
        // Execute first component and wait for the last component to finish the last tick.
        executeNext(fibers[0]);
    }

    /**
     * Execute next thread and wait for the current thread to execute again.
     */
    private void executeNext(Thread next) {
        current = next;
        LockSupport.unpark(next);
        waitForExecution();
    }

    /**
     * Wait for the current thread to execute.
     */
    private void waitForExecution() {
        var thread = Thread.currentThread();
        while (current != thread) {
            // There is no problem, if this thread is not parked when the previous threads unparks it:
            // In this case this park will not block, see LockSupport.unpark() javadoc.
            LockSupport.park(this);
            // park() does not block anymore, if the thread has been interrupted.
            abortIfInterrupted();
        }
    }

    @Override
    protected void doClose() {
        if (fibers != null) {
            terminate(List.of(fibers));
            fibers = null;
        }
    }
}
