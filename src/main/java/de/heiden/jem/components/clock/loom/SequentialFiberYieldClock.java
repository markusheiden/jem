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

    @Override
    protected void doInit() {
        var components = clockedComponents();
        var numComponents = components.length;

        // The thread running the clock executes first.
        state = numComponents;
        fibers = new Thread[numComponents];
        for (var i = 0; i < numComponents; i++) {
            var component = components[i];
            // Execute next component and wait for the next tick. The last component hands over to the clock.
            var state = i;
            var nextState = i + 1;
            component.setTick(() -> executeNext(nextState, state));
            fibers[i] = Thread.ofVirtual().name(component.getName()).start(() -> {
                // Wait for the first tick.
                waitForExecution(state);
                component.run();
            });
        }
    }

    @Override
    protected void doRun() {
        var finalState = fibers.length;
        //noinspection InfiniteLoopStatement
        while (true) {
            startTick();
            // Execute first component and wait for the last component to finish the tick.
            executeNext(0, finalState);
        }
    }

    @Override
    protected void doRun(int ticks) {
        assert ticks >= 0 : "Precondition: ticks >= 0";

        var finalState = fibers.length;
        for (long stop = getTick() + ticks; getTick() < stop; ) {
            startTick();
            // Execute first component and wait for the last component to finish the tick.
            executeNext(0, finalState);
        }
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
