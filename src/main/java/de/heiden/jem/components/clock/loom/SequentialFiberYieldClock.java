package de.heiden.jem.components.clock.loom;

import de.heiden.jem.components.clock.AbstractSimpleClock;
import de.heiden.jem.components.clock.ClockedComponent;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Clock using {@link Fiber}s from project loom.
 */
public final class SequentialFiberYieldClock extends AbstractSimpleClock {
    /**
     * The one and only thread to execute the fibers.
     */
    private final Executor executor = Executors.newSingleThreadExecutor();

    /**
     * Currently execute component.
     */
    private int state = -1;

    @Override
    protected final void doRun() {
        createFibers(-1).close();
    }

    @Override
    protected final void doRun(int ticks) {
        assert ticks >= 0 : "Precondition: ticks >= 0";

        createFibers(ticks).close();
    }

    /**
     * Create fibers.
     */
    private FiberScope createFibers(final int ticks) {
        ClockedComponent[] components = _componentMap.values().toArray(new ClockedComponent[0]);
        final int numComponents = components.length;

        FiberScope scope = FiberScope.open();
        var fibers = new ArrayList<Fiber<?>>(numComponents);

        final int finalState = numComponents;
        if (ticks < 0) {
            scope.schedule(executor, () -> {
                //noinspection InfiniteLoopStatement
                while (true) {
                    startTick();
                    // Execute first component and wait for last tick.
                    executeNextComponent(0, finalState);
                }
            });
        } else {
            scope.schedule(executor, () -> {
                for (final long stop = getTick() + ticks; getTick() < stop;) {
                    startTick();
                    // Execute first component and wait for last tick.
                    executeNextComponent(0, finalState);
                }
                fibers.forEach(Fiber::cancel);
            });
        }

        for (int i = 0; i < numComponents; i++) {
            ClockedComponent component = components[i];
            // Execute next component thread and wait for next tick.
            final int tickState = i;
            component.setTick(() -> executeNextComponent(tickState + 1, tickState));
            fibers.add(scope.schedule(executor, component::run));
        }

        return scope;
    }

    /**
     * Execute next component and wait for this component to execute.
     */
    private void executeNextComponent(final int nextState, final int state) {
        this.state = nextState;
        do {
            Thread.yield();
//            if (Thread.interrupted()) {
//                throw new ManualAbort();
//            }
        } while (this.state != state);
    }
}
