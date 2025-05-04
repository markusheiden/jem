package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.Tick;

/**
 * Clock implemented without synchronization sequentially executing component threads by using yield locks (busy wait).
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
        for (int state = 0; state < components.length; state++) {
            var component = components[state];
            var tick = new SequentialSpinTick(state);
            component.setTick(tick);

            // Start component.
            createStartedDaemonThread(component.getName(), () -> executeComponent(component, tick));
            // Wait for component to reach first tick.
            while (this.state != state + 1) {
                Thread.yield();
            }
        }

        // Start tick manager.
        createStartedDaemonThread("Tick", () -> executeTicks(components.length));
    }

    /**
     * Execution of ticks.
     */
    private void executeTicks(final int finalState) {
        //noinspection InfiniteLoopStatement
        while (true) {
            startTick();
            // Execute all component threads.
            state = 0;
            // Wait for all component threads to finish tick.
            do {
                Thread.yield();
            } while (state != finalState);
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
            final int tickState = this.tickState;
            // Execute next component thread.
            state = tickState + 1;
            // Wait for the next tick.
            do {
                Thread.yield();
            } while (state != tickState);
        }
    }
}
