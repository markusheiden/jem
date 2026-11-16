package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.Tick;

import static de.heiden.jem.components.clock.ManualAbort.abortIfInterrupted;

/**
 * Clock implemented without synchronization sequentially executing component threads by using yield locks (busy wait).
 * The thread of the last component starts the next tick, so there is no separate tick manager thread.
 */
public final class SequentialYieldClock extends AbstractSynchronizedClock {
    /**
     * Ordinal of component thread to execute.
     * Package visible to avoid synthetic accessors.
     */
    volatile int state = 0;

    @Override
    protected void doSynchronizedInit() {
        var components = clockedComponents();
        var last = components.length - 1;
        for (var state = 0; state < last; state++) {
            var component = components[state];
            var tick = new SequentialSpinTick(state);
            component.setTick(tick);

            // Start component.
            createStartedDaemonThread(component.getName(), () -> executeComponent(component, tick));
            // Wait for component to reach first tick.
            waitForState(state + 1);
        }

        // Start the last component. It starts the first tick when reaching its first tick.
        var component = components[last];
        Tick tick = () -> {
            startTick();
            // Execute first component thread.
            state = 0;
            // Wait for the next tick.
            waitForState(last);
        };
        component.setTick(tick);
        createStartedDaemonThread(component.getName(), () -> executeComponent(component, tick));
    }

    /**
     * Wait until the state is reached.
     * Package visible to avoid synthetic accessors.
     *
     * @param state
     *         State to reach.
     */
    void waitForState(int state) {
        while (this.state != state) {
            Thread.yield();
            abortIfInterrupted();
        }
    }

    /**
     * Special tick, waiting for its state.
     */
    private final class SequentialSpinTick implements Tick {
        /**
         * Ordinal of component thread.
         */
        private final int tickState;

        /**
         * Constructor.
         *
         * @param state
         *         Ordinal of component thread.
         */
        private SequentialSpinTick(int state) {
            this.tickState = state;
        }

        @Override
        public void waitForTick() {
            var tickState = this.tickState;
            // Execute next component thread.
            state = tickState + 1;
            // Wait for the next tick.
            waitForState(tickState);
        }
    }
}
