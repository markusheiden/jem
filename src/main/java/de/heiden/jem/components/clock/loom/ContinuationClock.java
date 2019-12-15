package de.heiden.jem.components.clock.loom;

import de.heiden.jem.components.clock.AbstractClock;
import de.heiden.jem.components.clock.ClockedComponent;

public class ContinuationClock extends AbstractClock {
    @Override
    protected final void doRun() {
        run(this::startTick);
    }

    @Override
    protected final void doRun(int ticks) {
        assert ticks >= 0 : "Precondition: ticks >= 0";

        Continuation[] continuations = createContinuations();

        for (long stop = _tick.get() + ticks ;_tick.get() < stop;) {
            for (Continuation continuation : continuations) {
                continuation.run();
            }
        }
    }

    /**
     * Execute runnables.
     *
     * @param startTick Runnable executed to start a new tick.
     */
    private void run(final Runnable startTick) {
        Continuation[] continuations = createContinuations();

        //noinspection InfiniteLoopStatement
        for (;;) {
            for (Continuation continuation : continuations) {
                continuation.run();
            }
        }
    }

    private Continuation[] createContinuations() {
        ContinuationScope[] scopes = new ContinuationScope[_componentMap.size()];
        Continuation[] continuations = new Continuation[_componentMap.size()];
        int i = 0;
        for (ClockedComponent component : _componentMap.values()) {
            final ContinuationScope scope = new ContinuationScope("component" + i);
            scopes[i] = scope;
            continuations[i] = new Continuation(scopes[i], () -> {
                component.run();
                Continuation.yield(scope);
            });
        }
        return continuations;
    }
}
