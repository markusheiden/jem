package de.heiden.jem.components.clock.loom;

import de.heiden.jem.components.clock.AbstractSimpleClock;
import de.heiden.jem.components.clock.ClockedComponent;
import de.heiden.jem.components.clock.ManualAbort;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Clock using {@link Thread fibers} from project loom.
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
        createFibers(-1);
    }

    @Override
    protected final void doRun(int ticks) {
        assert ticks >= 0 : "Precondition: ticks >= 0";

        createFibers(ticks);
    }

    /**
     * Create fibers.
     */
    private void createFibers(final int ticks) {
        ClockedComponent[] components = _componentMap.values().toArray(new ClockedComponent[0]);
        final int numComponents = components.length;

        var fibers = new ArrayList<Thread>(numComponents);

        final int finalState = numComponents;
        if (ticks < 0) {
            Thread.builder()
                    .virtual(executor)
                    .task(() -> {
                        //noinspection InfiniteLoopStatement
                        while (true) {
                            startTick();
                            // Execute first component and wait for last tick.
                            executeNextComponent(0, finalState);
                        }
                    })
                    .build()
                    .start();
        } else {
            Thread.builder()
                    .virtual(executor)
                    .task(() -> {
                        for (final long stop = getTick() + ticks; getTick() < stop; ) {
                            startTick();
                            // Execute first component and wait for last tick.
                            executeNextComponent(0, finalState);
                        }
                        fibers.forEach(Thread::interrupt);
                    })
                    .build()
                    .start();
        }

        for (int i = 0; i < numComponents; i++) {
            ClockedComponent component = components[i];
            // Execute next component thread and wait for next tick.
            final int tickState = i;
            final int nextTickState = i + 1;
            component.setTick(() -> executeNextComponent(nextTickState, tickState));
            Thread fiber = Thread.builder()
                    .virtual(executor)
                    .task(component::run)
                    .build();
            fiber.start();
            fibers.add(fiber);
        }
    }

    /**
     * Execute next component and wait for this component to execute.
     */
    private void executeNextComponent(final int nextState, final int state) {
        this.state = nextState;
        do {
            Thread.yield();
            if (Thread.interrupted()) {
                throw new ManualAbort();
            }
        } while (this.state != state);
    }
}
