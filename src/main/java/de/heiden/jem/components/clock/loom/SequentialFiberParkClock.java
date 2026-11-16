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

    @Override
    protected void doInit() {
        var components = clockedComponents();
        var numComponents = components.length;

        fibers = new Thread[numComponents];
        for (var i = 0; i < numComponents; i++) {
            var component = components[i];
            fibers[i] = Thread.ofVirtual().name(component.getName()).unstarted(() -> {
                // Wait for the first tick.
                waitForExecution();
                component.run();
            });
        }

        for (var i = 0; i < numComponents; i++) {
            // Execute next component and wait for the next tick. The last component hands over to the clock.
            var next = i + 1;
            components[i].setTick(() -> executeNext(next < numComponents ? fibers[next] : clockThread));
        }

        for (var fiber : fibers) {
            fiber.start();
        }
    }

    @Override
    protected void doRun() {
        clockThread = Thread.currentThread();
        var firstFiber = fibers[0];
        //noinspection InfiniteLoopStatement
        while (true) {
            startTick();
            // Execute first component and wait for the last component to finish the tick.
            executeNext(firstFiber);
        }
    }

    @Override
    protected void doRun(int ticks) {
        assert ticks >= 0 : "Precondition: ticks >= 0";

        clockThread = Thread.currentThread();
        var firstFiber = fibers[0];
        for (long stop = getTick() + ticks; getTick() < stop; ) {
            startTick();
            // Execute first component and wait for the last component to finish the tick.
            executeNext(firstFiber);
        }
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
