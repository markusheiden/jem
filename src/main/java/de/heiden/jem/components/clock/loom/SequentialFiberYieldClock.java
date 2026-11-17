package de.heiden.jem.components.clock.loom;

import de.heiden.jem.components.clock.AbstractSimpleClock;

import java.util.List;

import static de.heiden.jem.components.clock.ManualAbort.abortIfInterrupted;

/**
 * Clock using {@link Thread fibers} from project loom.
 * The thread running the clock and the fibers of the components hand over execution via a state and yield.
 */
public final class SequentialFiberYieldClock extends AbstractSimpleClock {
    /**
     * Fibers of the components.
     */
    private Thread[] fibers;

    /**
     * Ordinal of the component to execute.
     * The number of components denotes the thread running the clock.
     */
    private volatile int state;

    /**
     * Last tick to execute.
     * Written by the clock thread before handing over execution to the fibers.
     */
    private long stop;

    @Override
    protected void doInit() {
        var components = clockedComponents();
        var numComponents = components.length;

        // The thread running the clock executes first.
        state = numComponents;
        fibers = new Thread[numComponents];
        for (var i = 0; i < numComponents; i++) {
            var component = components[i];
            var state = i;
            var nextState = i + 1;
            if (nextState < numComponents) {
                // Execute next component and wait for the next tick.
                component.setTick(() -> executeNext(nextState, state));
            } else {
                // The last component starts the next tick itself to avoid handing over to the clock for each tick.
                component.setTick(() -> {
                    if (getTick() < stop) {
                        startTick();
                        executeNext(0, state);
                    } else {
                        executeNext(nextState, state);
                    }
                });
            }
            fibers[i] = Thread.ofVirtual().name(component.getName()).start(() -> runUntilAbort(() -> {
                // Wait for the first tick.
                waitForExecution(state);
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
        executeNext(0, fibers.length);
    }

    /**
     * Execute next component and wait for this component to execute again.
     */
    private void executeNext(int nextState, int state) {
        this.state = nextState;
        waitForExecution(state);
    }

    /**
     * Wait for the component to execute.
     */
    private void waitForExecution(int state) {
        while (this.state != state) {
            Thread.yield();
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
