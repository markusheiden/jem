package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.Tick;

import java.util.concurrent.atomic.AtomicInteger;

import static de.heiden.jem.components.clock.ManualAbort.abortIfInterrupted;
import static java.lang.Thread.onSpinWait;

/**
 * Clock implemented without synchronization sequentially executing component threads by using spin locks (busy wait).
 * The thread of the last component starts the next tick, so there is no separate tick manager thread.
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
        var last = components.length - 1;
        for (var state = 0; state < last; state++) {
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

        // Start the last component. It starts the first tick when reaching its first tick.
        var component = components[last];
        Tick tick = () -> {
            startTick();
            // Make all changes of this thread visible to all other threads because of release semantics.
            // Trigger execution of the first component's thread.
            currentState.setRelease(0);
            // Wait for the next tick.
            waitForState(last, currentState);
        };
        component.setTick(tick);
        createStartedDaemonThread(component.getName(), () -> executeComponent(component, tick));
    }

    /**
     * Wait until the state is reached.
     *
     * @param state
     *         State to reach.
     * @param currentState
     *         Current state.
     */
    private static void waitForState(int state, AtomicInteger currentState) {
        do {
            onSpinWait();
            abortIfInterrupted();
            // Make released changes of all other threads visible to this thread because of acquire semantics.
        } while (currentState.getAcquire() != state);
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
            // Wait for the next tick.
            waitForState(state, currentState);
        }
    }
}
