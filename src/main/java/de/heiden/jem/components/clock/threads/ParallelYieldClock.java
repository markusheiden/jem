package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.Tick;

import static de.heiden.jem.components.clock.ManualAbort.abortIfInterrupted;
import static java.lang.Thread.onSpinWait;

/**
 * Clock implemented without synchronization sequentially executing components by using yield (busy wait).
 * Before yielding the threads spin for a while, if there are enough processors.
 */
public final class ParallelYieldClock extends AbstractSynchronizedClock {
    @Override
    protected void doSynchronizedInit() {
        var components = clockedComponents();
        var ticks = new ParallelSpinTick[components.length];
        for (var i = 0; i < components.length; i++) {
            var component = components[i];
            var tick = new ParallelSpinTick(spinLimit());
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
    private void executeTicks(ParallelSpinTick[] ticks) {
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

        /**
         * Number of spins to wait for the state before yielding.
         */
        private final int spinLimit;

        /**
         * Constructor.
         *
         * @param spinLimit
         *         Number of spins to wait for the state before yielding.
         */
        private ParallelSpinTick(int spinLimit) {
            this.spinLimit = spinLimit;
        }

        @Override
        public void waitForTick() {
            tickEnd = true;
            waitFor(false);
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
            waitFor(true);
        }

        /**
         * Wait until the state is reached.
         * First busy waits, because yielding is expensive.
         */
        private void waitFor(boolean tickEnd) {
            for (var i = spinLimit; i > 0; i--) {
                if (this.tickEnd == tickEnd) {
                    return;
                }
                onSpinWait();
            }
            while (this.tickEnd != tickEnd) {
                Thread.yield();
                abortIfInterrupted();
            }
        }
    }
}
