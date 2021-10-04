package de.heiden.jem.components.clock.loom;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;

import de.heiden.jem.components.clock.AbstractSimpleClock;
import de.heiden.jem.components.clock.ClockedComponent;

/**
 * Clock using {@link Thread fibers} from project loom.
 */
public final class SequentialFiberParkClock extends AbstractSimpleClock {
    /**
     * The one and only thread to execute the fibers.
     */
    private final Executor executor = Executors.newSingleThreadExecutor();

    /**
     * Currently executed component.
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
        var components = _componentMap.values().toArray(new ClockedComponent[0]);
        var numComponents = components.length;

        var fibers = new ArrayList<Thread>(numComponents + 1);
        for (int i = 0; i < numComponents; i++) {
            var component = components[i];
            // Execute next component thread and wait for next tick.
            var tickState = i;
            var nextTickState = i + 1;
            component.setTick(() -> executeNextComponent(nextTickState, fibers, tickState));
            var fiber = Thread.ofVirtual()
                    .scheduler(executor)
                    .unstarted(component::run);
            fibers.add(fiber);
        }

        if (ticks < 0) {
            var fiber = Thread.ofVirtual()
                    .scheduler(executor)
                    .unstarted(() -> {
                        //noinspection InfiniteLoopStatement
                        while (true) {
                            startTick();
                            // Execute first component and wait for last tick.
                            executeNextComponent(0, fibers, numComponents);
                        }
                    });
            fibers.add(fiber);
        } else {
            var fiber = Thread.ofVirtual()
                    .scheduler(executor)
                    .unstarted(() -> {
                        for (final long stop = getTick() + ticks; getTick() < stop; ) {
                            startTick();
                            // Execute first component and wait for last tick.
                            executeNextComponent(0, fibers, numComponents);
                        }
                        fibers.forEach(Thread::interrupt);
                    });
            fibers.add(fiber);
        }

        fibers.forEach(Thread::start);
    }

    /**
     * Execute next component and wait for this component to execute.
     */
    private void executeNextComponent(final int nextState, final List<Thread> fibers, final int state) {
        this.state = nextState;
        LockSupport.unpark(fibers.get(nextState));
        LockSupport.park();
//        if (Thread.interrupted()) {
//            throw new ManualAbort();
//        }
        while (this.state != state) {
            Thread.yield();
        } ;
    }
}
