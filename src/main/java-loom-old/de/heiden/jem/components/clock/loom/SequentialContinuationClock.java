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
    protected void doRun() {
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
    protected void doRun(int ticks) {
        assert ticks >= 0 : "Precondition: ticks >= 0";

        var continuations = createContinuations();
        for (final long stop = getTick() + ticks; getTick() < stop; ) {
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
        var components = _componentMap.values().toArray(ClockedComponent[]::new);

        var scope = new ContinuationScope("Clock");
        var continuations = new Continuation[components.length];
        for (int i = 0; i < components.length; i++) {
            var component = components[i];
            component.setTick(() -> Continuation.yield(scope));
            continuations[i] = new Continuation(scope, component::run);
        }

        return continuations;
    }
}
