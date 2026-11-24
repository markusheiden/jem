package de.heiden.jem.components.clock.loom;

import de.heiden.jem.components.clock.AbstractSimpleClock;
import jdk.internal.vm.Continuation;
import jdk.internal.vm.ContinuationScope;
import org.jspecify.annotations.Nullable;

import static de.heiden.jem.components.clock.ManualAbort.abortIfInterrupted;
import static java.util.Objects.requireNonNull;

/**
 * Clock using the continuations, which {@link Thread fibers} from project loom are built on.
 * The thread running the clock executes all components and events itself:
 * Each component is a continuation, that yields at the end of its tick.
 * <p>
 * Continuations are an internal API of the JDK,
 * so this clock needs the JVM argument "--add-exports java.base/jdk.internal.vm=ALL-UNNAMED".
 * Components must not wait for a tick while holding a monitor.
 */
public final class ContinuationClock extends AbstractSimpleClock {
    /**
     * Scope of the continuations of the components.
     */
    private static final ContinuationScope SCOPE = new ContinuationScope("Clock");

    /**
     * Mask for the ticks at which an interrupt of the thread running the clock is checked.
     * Checking only every 65536 ticks avoids reading the interrupt status per tick.
     */
    private static final long ABORT_CHECK_MASK = 0xFFFF;

    /**
     * Continuations of the components.
     */
    private Continuation @Nullable [] continuations;

    @Override
    protected void doInit() {
        var components = clockedComponents();
        var continuations = new Continuation[components.length];
        for (var i = 0; i < components.length; i++) {
            var component = components[i];
            // Hand over execution to the thread running the clock, which executes the next component.
            component.setTick(() -> Continuation.yield(SCOPE));
            continuations[i] = new Continuation(SCOPE, component::run);
        }
        this.continuations = continuations;
    }

    @Override
    protected void doRun() {
        execute(Long.MAX_VALUE);
    }

    @Override
    protected void doRun(int ticks) {
        assert ticks >= 0 : "Precondition: ticks >= 0";

        execute(getTick() + ticks);
    }

    /**
     * Execute ticks until the given tick.
     */
    private void execute(long stop) {
        var continuations = requireNonNull(this.continuations, "Clock is not started or already closed");
        while (getTick() < stop) {
            startTick();
            for (var continuation : continuations) {
                // Execute the component until it waits for the next tick.
                continuation.run();
            }
            if ((getTick() & ABORT_CHECK_MASK) == 0) {
                abortIfInterrupted();
            }
        }
    }

    @Override
    protected void doClose() {
        // Continuations are not bound to any resources, so it is sufficient to drop them.
        continuations = null;
    }
}
