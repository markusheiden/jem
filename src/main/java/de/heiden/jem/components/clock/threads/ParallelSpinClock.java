package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.Tick;

/**
 * Clock implemented without synchronization sequentially executing components by using spin locks (busy wait).
 */
public final class ParallelSpinClock extends AbstractSynchronizedClock {
    @Override
    protected void doSynchronizedInit() {
        var components = clockedComponents();
        var ticks = new ParallelSpinTick[components.length];
        for (int i = 0; i < components.length; i++) {
            var component = components[i];
            var tick = new ParallelSpinTick();
            component.setTick(tick);
            ticks[i] = tick;

            // Start component.
            createStartedDaemonThread(component.getName(), () -> executeComponent(component, tick));
            // Wait for component to reach first tick.
            tick.waitForTickEnd();
        }

        // Start tick manager.
        createStartedDaemonThread("Tick", () -> executeTicks(ticks));
    }

    /**
     * Execution of ticks.
     */
    private void executeTicks(final ParallelSpinTick[] ticks) {
        //noinspection InfiniteLoopStatement
        while (true) {
            // Start a new tick.
            startTick();
            // Execute all component threads.
            for (var tick : ticks) {
                tick.startTick();
            }
            // Wait for all component threads to finish the tick.
            for (var tick : ticks) {
                tick.waitForTickEnd();
            }
        }
    }

    /**
     * Special tick, waiting for its state.
     */
    private static final class ParallelSpinTick implements Tick {
        /**
         * State of tick.
         * True: Current tick finished, waiting for the next tick.
         * False: Start next tick.
         */
        private volatile boolean tickEnd = false;

        @Override
        public void waitForTick() {
            tickEnd = true;
            do {
                Thread.onSpinWait();
            } while (tickEnd);
        }

        /**
         * Start the next tick.
         */
        void startTick() {
            tickEnd = false;
        }

        /**
         * Wait for tick to finish.
         */
        void waitForTickEnd() {
            while (!tickEnd) {
                Thread.onSpinWait();
            }
        }
    }
}
