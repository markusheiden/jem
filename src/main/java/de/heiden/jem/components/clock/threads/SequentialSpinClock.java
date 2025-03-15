package de.heiden.jem.components.clock.threads;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import de.heiden.jem.components.clock.Tick;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.onSpinWait;

/**
 * Clock implemented without synchronization sequentially executing component threads by using spin locks (busy wait).
 */
public final class SequentialSpinClock extends AbstractSynchronizedClock {
    /**
     * Ordinal of component thread to execute.
     */
    private final AtomicInteger _state = new AtomicInteger(0);

    @Override
    protected void doSynchronizedInit() {
        var components = new ArrayList<>(_componentMap.values());
        for (int tickState = 0; tickState < components.size(); tickState++) {
            var component = components.get(tickState);
            var tick = new SequentialSpinTick(tickState, _state);
            component.setTick(tick);

            // Start component.
            createStartedDaemonThread(component.getName(), () -> executeComponent(component, tick));
            // Wait for the component to reach the first tick.
            while (_state.get() != tickState + 1) {
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
        final Thread thread = currentThread();
        final AtomicInteger state = _state;
        while (!thread.isInterrupted()) {
            startTick();
            // Execute all component threads.
            state.set(0);
            // Wait for all component threads to finish the tick.
            do {
                onSpinWait();
            } while (state.get() != finalState);
        }
    }

    /**
     * Special tick, waiting for its state.
     *
     * @param _tickState
     *         Ordinal of component thread.
     * @param _state
     *         Current state.
     */
    private record SequentialSpinTick(int _tickState, AtomicInteger _state) implements Tick {
        @Override
        public void waitForTick() {
            synchronized (_state) {
                // Make changes of this thread visible to all other threads.
            }
            // Trigger execution of the next component thread.
            _state.set(_tickState + 1);
            // Wait for the next tick.
            do {
                onSpinWait();
            } while (_state.get() != _tickState);
        }
    }
}
