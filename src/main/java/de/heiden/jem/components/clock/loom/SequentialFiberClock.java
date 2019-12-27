package de.heiden.jem.components.clock.loom;

import de.heiden.jem.components.clock.AbstractSimpleClock;
import de.heiden.jem.components.clock.ClockedComponent;
import de.heiden.jem.components.clock.Tick;

import java.util.concurrent.locks.LockSupport;

/**
 * Clock using {@link Fiber}s from project loom.
 */
public final class SequentialFiberClock extends AbstractSimpleClock {
    @Override
    protected final void doRun() {
        createFibers().close();
    }

    @Override
    protected final void doRun(int ticks) {
        assert ticks >= 0 : "Precondition: ticks >= 0";

//        final var fibers = createFibers();
//        for (final long stop = getTick() + ticks; getTick() < stop;) {
//            startTick();
//            for (final Fiber fiber : fibers) {
//                continuation.run();
//            }
//        }
    }

    /**
     * Create fibers.
     */
    private FiberScope createFibers() {
        ClockedComponent[] components = _componentMap.values().toArray(new ClockedComponent[0]);
        FiberTick[] ticks = new FiberTick[components.length + 1];
        Fiber[] fibers = new Fiber[components.length + 1];

        FiberScope scope = FiberScope.open();
        ticks[0] = new FiberTick();
        fibers[0] = scope.schedule(this::startTick);

        for (int i = 0; i < components.length; i++) {
            ClockedComponent component = components[i];
            FiberTick tick = new FiberTick();
            component.setTick(tick);
            ticks[i + 1] = tick;
            fibers[i + 1] = scope.schedule(component::run);
        }

        for (int i = 0; i < fibers.length; i++) {
            int previous = i > 0 ? i - 1 : fibers.length - 1;
            ticks[i].next = fibers[previous];
        }

        return scope;
    }

    static class FiberTick implements Tick {
        private Fiber next;

        @Override
        public void waitForTick() {
            LockSupport.park();
            LockSupport.unpark(next);
        }
    }
}
