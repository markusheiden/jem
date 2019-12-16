package de.heiden.jem.components.clock.loom;

import de.heiden.jem.components.clock.AbstractClock;
import de.heiden.jem.components.clock.ClockedComponent;

import static java.lang.Continuation.yield;

/**
 * Clock using {@link Continuation}s from project loom.
 */
public final class ContinuationClock extends AbstractClock {
    @Override
    protected final void doRun() {
        final var continuations = createContinuations();
        //noinspection InfiniteLoopStatement
        for (;;) {
            startTick();
            for (Continuation continuation : continuations) {
                continuation.run();
            }
        }
    }

    @Override
    protected final void doRun(int ticks) {
        assert ticks >= 0 : "Precondition: ticks >= 0";

        final var continuations = createContinuations();
        for (final long stop = _tick.get() + ticks; _tick.get() < stop;) {
            startTick();
            for (Continuation continuation : continuations) {
                continuation.run();
            }
        }
    }

    /**
     * Create continuations.
     */
    private Continuation[] createContinuations() {
        ClockedComponent[] components = _componentMap.values().toArray(new ClockedComponent[0]);
        Continuation[] continuations = new Continuation[components.length];
        for (int i = 0; i < components.length; i++) {
            ClockedComponent component = components[i];
            final var scope = new ContinuationScope("Component " + i + ": " + component.getName());
            component.setTick(() -> yield(scope));
            continuations[i] = new Continuation(scope, component::run);
        }
        return continuations;
    }
}
