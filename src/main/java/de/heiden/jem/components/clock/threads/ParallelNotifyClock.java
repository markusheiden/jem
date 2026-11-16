package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.ManualAbort;
import de.heiden.jem.components.clock.Tick;

import static java.lang.Thread.onSpinWait;

/**
 * Clock implemented with synchronization, executing component threads in parallel by using wait / notify.
 */
public final class ParallelNotifyClock extends AbstractSynchronizedClock {
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
            // Wait for all component threads to finish tick.
            for (var tick : ticks) {
                tick.waitForTickEnd();
            }
        }
    }

    /**
     * Special tick, waiting for its state.
     * First busy waits, because blocking and notifying a thread is expensive.
     */
    private static final class ParallelSpinTick implements Tick {
        /**
         * State of tick.
         * True: Current tick finished, waiting for next tick.
         * False: Start next tick.
         */
        private volatile boolean state = false;

        /**
         * Number of spins to wait for the state before blocking.
         */
        private final int spinLimit;

        /**
         * Constructor.
         *
         * @param spinLimit
         *         Number of spins to wait for the state before blocking.
         */
        private ParallelSpinTick(int spinLimit) {
            this.spinLimit = spinLimit;
        }

        @Override
        public void waitForTick() {
            setState(true);
            waitForState(false);
        }

        /**
         * Start next tick.
         */
        void startTick() {
            setState(false);
        }

        /**
         * Wait for tick to finish.
         */
        void waitForTickEnd() {
            waitForState(true);
        }

        /**
         * Set state and notify the thread waiting for it, if it is blocked.
         */
        private void setState(boolean state) {
            this.state = state;
            synchronized (this) {
                notifyAll();
            }
        }

        /**
         * Wait until the state is reached.
         */
        private void waitForState(boolean state) {
            for (var i = spinLimit; i > 0; i--) {
                if (this.state == state) {
                    return;
                }
                onSpinWait();
            }
            synchronized (this) {
                try {
                    while (this.state != state) {
                        wait();
                    }
                } catch (InterruptedException e) {
                    throw new ManualAbort();
                }
            }
        }
    }
}
