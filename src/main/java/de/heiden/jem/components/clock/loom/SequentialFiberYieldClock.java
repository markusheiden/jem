package de.heiden.jem.components.clock.loom;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.heiden.jem.components.clock.AbstractSimpleClock;
import de.heiden.jem.components.clock.ManualAbort;

/**
 * Clock using {@link Thread fibers} from project loom.
 */
public final class SequentialFiberYieldClock extends AbstractSimpleClock {
    /**
     * The one and only thread to execute the fibers.
     */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Currently execute component.
     */
    private int state = -1;

    @Override
    protected void doRun() {
        createFibers(-1);
    }

    @Override
    protected void doRun(int ticks) {
        assert ticks >= 0 : "Precondition: ticks >= 0";

        createFibers(ticks);
    }

    /**
     * Create fibers.
     */
    private void createFibers(final int ticks) {
        var components = clockedComponents();
        var numComponents = components.length;

        var fibers = new ArrayList<Thread>(numComponents);

        var finalState = numComponents;
        if (ticks < 0) {
            buildVirtualThread(() -> {
                //noinspection InfiniteLoopStatement
                while (true) {
                    startTick();
                    // Execute first component and wait for last tick.
                    executeNextComponent(0, finalState);
                }
            });
        } else {
            buildVirtualThread(() -> {
                for (final long stop = getTick() + ticks; getTick() < stop; ) {
                    startTick();
                    // Execute first component and wait for last tick.
                    executeNextComponent(0, finalState);
                }
                fibers.forEach(Thread::interrupt);
            });
        }

        for (int i = 0; i < numComponents; i++) {
            var component = components[i];
            // Execute next component thread and wait for next tick.
            var state = i;
            var nextState = i + 1;
            component.setTick(() -> executeNextComponent(nextState, state));
            var fiber = buildVirtualThread(component::run);
            fibers.add(fiber);
        }
    }

    private Thread buildVirtualThread(Runnable task) {
        return Thread.ofVirtual()
//                .scheduler(executor)
                .start(task);
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

    @Override
    protected void doClose() {
        executor.shutdown();
    }
}
