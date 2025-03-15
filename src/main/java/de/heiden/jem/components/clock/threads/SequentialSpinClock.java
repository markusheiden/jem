package de.heiden.jem.components.clock.threads;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import de.heiden.jem.components.clock.Tick;

import static java.lang.Thread.onSpinWait;

/**
 * Clock implemented without synchronization sequentially executing component threads by using spin locks (busy wait).
 */
public final class SequentialSpinClock extends AbstractSynchronizedClock {
    /**
     * Ordinal of component thread to execute.
     * Package visible to avoid synthetic accessors.
     */
    final AtomicInteger _state = new AtomicInteger(0);

    /**
     * Needed to make all changes visible to all threads that use this clock.
     * Package visible to avoid synthetic accessors.
     */
    final Object _lock = new Object();

    @Override
    protected void doSynchronizedInit() {
        var components = new ArrayList<>(_componentMap.values());
        for (int state = 0; state < components.size(); state++) {
            var component = components.get(state);
            var tick = new SequentialSpinTick(state);
            component.setTick(tick);

            // Start component.
            createStartedDaemonThread(component.getName(), () -> executeComponent(component, tick));
            // Wait for component to reach first tick.
            while (_state.get() != state + 1) {
                onSpinWait();
            }
        }

        // Start tick manager.
        createStartedDaemonThread("Tick", () -> executeTicks(components.size()));
    }

    /**
     * Execution of ticks.
     */
    private void executeTicks(final int finalState) {
        var thread = Thread.currentThread();
        while (!thread.isInterrupted()) {
            startTick();
            // Execute all component threads.
            _state.set(0);
            // Wait for all component threads to finish tick.
            do {
                onSpinWait();
            } while (_state.get() != finalState);
        }
    }

    /**
     * Special tick, waiting for its state.
     */
    private final class SequentialSpinTick implements Tick {
        /**
         * Ordinal of component thread.
         */
        private final int _tickState;

        /**
         * Constructor.
         *
         * @param state
         *         Ordinal of component thread.
         */
        private SequentialSpinTick(int state) {
            this._tickState = state;
        }

        @Override
        public void waitForTick() {
            final int tickState = _tickState;
            // Make changes of this thread visible to all other threads.
            synchronized (_lock) {
                // Trigger execution of the next component thread.
                _state.set(tickState + 1);
            }
            // Wait for the next tick.
            do {
                onSpinWait();
            } while (_state.get() != tickState);
        }
    }
}
