package de.heiden.jem.components.clock.loom;

import de.heiden.jem.components.clock.AbstractSimpleClock;
import de.heiden.jem.components.clock.ClockedComponent;
import jdk.internal.vm.Continuation;
import jdk.internal.vm.ContinuationScope;

/**
 * Clock using {@link Continuation}s from project loom.
 */
public final class SequentialContinuationClock extends AbstractSimpleClock {
    @Override
    protected final void doRun() {
        var continuations = createContinuations();
        //noinspection InfiniteLoopStatement
        while (true) {
            startTick();
            for (var continuation : continuations) {
                continuation.run();
            }
        }
    }

    @Override
    protected final void doRun(int ticks) {
        assert ticks >= 0 : "Precondition: ticks >= 0";

        var continuations = createContinuations();
        for (final long stop = getTick() + ticks; getTick() < stop;) {
            startTick();
            for (var continuation : continuations) {
                continuation.run();
            }
        }
    }

    /**
     * Create continuations.
     */
    private Continuation[] createContinuations() {
        var components = _componentMap.values().toArray(new ClockedComponent[0]);
        var continuations = new Continuation[components.length];
        for (int i = 0; i < components.length; i++) {
            final var component = components[i];
            final var scope = new ContinuationScope("Component " + i + ": " + component.getName());
            component.setTick(() -> Continuation.yield(scope));
            continuations[i] = new Continuation(scope, component::run);
        }
        return continuations;
    }
}
