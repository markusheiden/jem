package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.Tick;
import de.heiden.jem.components.clock.loom.FiberScheduler;
import de.heiden.jem.components.clock.loom.YieldHandover;

import static java.lang.Thread.onSpinWait;

/**
 * Clock implemented without synchronization sequentially executing component threads by using yield locks (busy wait).
 * Before yielding the threads spin for a while, if there are enough processors.
 * Threads waiting for long park instead of yielding.
 * The thread of the last component starts the next tick, so there is no separate tick manager thread.
 * <p>
 * Executed on a {@link FiberScheduler#useSingleCarrier() single carrier thread},
 * all components and events are executed by the same thread.
 */
public final class SequentialYieldClock extends AbstractSynchronizedClock {
    /**
     * Handover of execution between the component threads by their ordinal.
     * While starting the component threads, the starting thread waits with the ordinal of the next component thread.
     * Package visible to avoid synthetic accessors.
     */
    YieldHandover handover;

    @Override
    protected void doSynchronizedInit() {
        logCarrier();

        var components = clockedComponents();
        var last = components.length - 1;
        handover = new YieldHandover(components.length, components.length, 0);
        for (var state = 0; state < last; state++) {
            var component = components[state];
            var tick = new SequentialYieldTick(state);
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
            handover.resume(0);
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
        var handover = this.handover;
        // First busy wait, because yielding is expensive.
        for (var i = spinLimit(); i > 0; i--) {
            if (handover.isCurrent(state)) {
                return;
            }
            onSpinWait();
        }
        handover.await(state);
    }

    /**
     * Special tick, waiting for its state.
     */
    private final class SequentialYieldTick implements Tick {
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
        private SequentialYieldTick(int state) {
            this.tickState = state;
        }

        @Override
        public void waitForTick() {
            var tickState = this.tickState;
            // Execute next component thread.
            handover.resume(tickState + 1);
            // Wait for the next tick.
            waitForState(tickState);
        }
    }
}
