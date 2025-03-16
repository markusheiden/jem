package de.heiden.jem.components.clock.threads;

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
     * Used as monitor for visibility guarantees too.
     */
    private final AtomicInteger _state = new AtomicInteger(0);

    /**
     * Tick manager thread.
     */
    private Thread _tickManagerThread;

    /**
     * {@link #clockedComponents()} threads.
     */
    private Thread[] _componentThreads;

    @Override
    protected void doSynchronizedInit() {
        var components = clockedComponents();
        _componentThreads = new Thread[components.length];
        for (int state = 0; state < components.length; state++) {
            var component = components[state];
            var tick = new SequentialSpinTick(state, _state);
            component.setTick(tick);

            // Start component.
            _componentThreads[state] =
                    createStartedDaemonThread(component.getName(), () -> executeComponent(component, tick));
            // Wait for the component to reach the first tick.
            while (_state.get() != state + 1) {
                onSpinWait();
            }
        }

        // Start tick manager.
        _tickManagerThread = createStartedDaemonThread("Tick manager", () -> executeTicks(components.length));
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

    @Override
    protected void doClose() {
        super.doClose();
        join(_tickManagerThread);
        var components = clockedComponents();
        for (int state = 0; state < components.length; state++) {
            _state.set(state);
            join(_componentThreads[state]);
        }
    }

    /**
     * Ensure that thread has been joined.
     */
    private void join(Thread thread) {
        boolean failed = true;
        while (failed) {
            try {
                thread.join();
                failed = false;
            } catch (InterruptedException e) {
                // Ignore to ensure that all threads terminate.
            }
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
            // Make all changes of this thread visible to all other threads because of release semantics.
            // Trigger execution of the next component thread.
            _state.setRelease(_tickState + 1);
            // Wait for the next tick.
            do {
                onSpinWait();
                // Make released changes of all other threads visible to this thread because of acquire semantics.
                // Wait for this component's turn.
            } while (_state.getAcquire() != _tickState);
        }
    }
}
