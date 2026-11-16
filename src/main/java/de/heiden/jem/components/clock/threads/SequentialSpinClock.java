package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.Tick;

import java.util.concurrent.atomic.AtomicInteger;

import static de.heiden.jem.components.clock.ManualAbort.abortIfInterrupted;
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

    @Override
    protected void doSynchronizedInit() {
        var components = clockedComponents();
        for (var state = 0; state < components.length; state++) {
            var component = components[state];
            var tick = new SequentialSpinTick(state, currentState);
            component.setTick(tick);

            // Start component.
            createStartedDaemonThread(component.getName(), () -> executeComponent(component, tick));
            // Wait for the component to reach the first tick.
            while (currentState.get() != state + 1) {
                onSpinWait();
                abortIfInterrupted();
            }
        }

        // Start tick manager.
        createStartedDaemonThread("Tick manager", () -> executeTicks(components.length));
    }

    /**
     * Execution of ticks.
     */
    private void executeTicks(int finalState) {
        var state = currentState;
        //noinspection InfiniteLoopStatement
        while (true) {
            startTick();
            // Execute all component threads.
            state.setRelease(0);
            // Wait for all component threads to finish the tick.
            do {
                onSpinWait();
                abortIfInterrupted();
            } while (state.getAcquire() != finalState);
        }
    }

    /**
     * Special tick, waiting for its state.
     *
     * @param state
     *         Ordinal of component thread.
     * @param currentState
     *         Current state.
     */
    private record SequentialSpinTick(int state, AtomicInteger currentState) implements Tick {
        @Override
        public void waitForTick() {
            // Make all changes of this thread visible to all other threads because of release semantics.
            // Trigger execution of the next component's thread.
            currentState.setRelease(state + 1);
            do {
                onSpinWait();
                abortIfInterrupted();
                // Make released changes of all other threads visible to this thread because of acquire semantics.
                // Wait for the next tick.
            } while (currentState.getAcquire() != state);
        }
    }
}
