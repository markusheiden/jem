package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.Tick;

import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.onSpinWait;

/**
 * Clock implemented without synchronization sequentially executing component threads by using spin locks (busy wait).
 */
public final class SequentialSpinClock extends AbstractSynchronizedClock {
    /**
     * Ordinal of component thread to execute.
     * Used as a monitor for visibility guarantees too.
     */
    private final AtomicInteger currentState = new AtomicInteger(0);

    /**
     * Tick manager thread.
     */
    private Thread tickManagerThread;

    /**
     * {@link #clockedComponents()} threads.
     */
    private Thread[] componentThreads;

    @Override
    protected void doSynchronizedInit() {
        var components = clockedComponents();
        componentThreads = new Thread[components.length];
        for (int state = 0; state < components.length; state++) {
            var component = components[state];
            var tick = new SequentialSpinTick(state, currentState);
            component.setTick(tick);

            // Start component.
            componentThreads[state] =
                    createStartedDaemonThread(component.getName(), () -> executeComponent(component, tick));
            // Wait for the component to reach the first tick.
            while (currentState.get() != state + 1) {
                onSpinWait();
            }
        }

        // Start tick manager.
        tickManagerThread = createStartedDaemonThread("Tick manager", () -> executeTicks(components.length));
    }

    /**
     * Execution of ticks.
     */
    private void executeTicks(final int finalState) {
        final Thread thread = currentThread();
        final AtomicInteger state = currentState;
        while (!thread.isInterrupted()) {
            startTick();
            // Execute all component threads.
            state.setRelease(0);
            // Wait for all component threads to finish the tick.
            do {
                onSpinWait();
            } while (state.getAcquire() != finalState);
        }
    }

    @Override
    protected void doClose() {
        super.doClose();
        var components = clockedComponents();
        var finalState = components.length;
        for (int state = 0; state < finalState; state++) {
            currentState.set(state);
            join(componentThreads[state]);
        }
        currentState.set(finalState);
        join(tickManagerThread);
    }

    /**
     * Ensure that the thread has been joined.
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
     * @param state
     *         Ordinal of component thread.
     * @param _currentState
     *         Current state.
     */
    private record SequentialSpinTick(int state, AtomicInteger _currentState) implements Tick {
        @Override
        public void waitForTick() {
            // Make all changes of this thread visible to all other threads because of release semantics.
            // Trigger execution of the next component's thread.
            _currentState.setRelease(state + 1);
            do {
                onSpinWait();
                // Make released changes of all other threads visible to this thread because of acquire semantics.
                // Wait for the next tick.
            } while (_currentState.getAcquire() != state);
        }
    }
}
