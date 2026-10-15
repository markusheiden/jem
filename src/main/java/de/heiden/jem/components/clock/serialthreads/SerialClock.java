package de.heiden.jem.components.clock.serialthreads;

import de.heiden.jem.components.clock.AbstractSimpleClock;
import de.heiden.jem.components.clock.ClockedComponent;
import de.heiden.jem.components.clock.Tick;
import org.serialthreads.Executor;
import org.serialthreads.Interrupt;
import org.serialthreads.context.ThreadFinishedException;

/**
 * Clock using serial threads.
 * This clock does NOT {@link ClockedComponent#setTick(Tick) set a tick},
 * because {@link Tick#waitForTick()} is never called because it is an {@link Interrupt} method.
 */
public final class SerialClock extends AbstractSimpleClock {
    /**
     * Mask for the ticks at which a stop request is checked.
     * Checking only every 65536 ticks avoids a volatile read per tick.
     */
    private static final long STOP_CHECK_MASK = 0xFFFF;

    /**
     * Has a stop been requested?.
     */
    private volatile boolean stopRequested = false;

    /**
     * Request the running clock to stop.
     * The running clock stops within the next 65536 ticks.
     * Each run resets the request, so a stop requested before a run starts is ignored.
     * May be called from any thread.
     */
    public void stop() {
        stopRequested = true;
    }

    /**
     * Has a stop been requested?
     * Checks the stop request only every 65536 ticks to avoid a volatile read per tick.
     */
    private boolean isStopRequested() {
        return (getTick() & STOP_CHECK_MASK) == 0 && stopRequested;
    }

    @Override
    @Executor
    protected void doRun() {
        stopRequested = false;
        var components = clockedComponents();
        try {
            while (!isStopRequested()) {
                startTick();
                for (var runnable : components) {
                    runnable.run();
                }
            }
        } catch (ThreadFinishedException e) {
            // TODO 2009-12-11 mh: should not happen!!!
        }
    }

    @Override
    @Executor
    protected void doRun(int ticks) {
        assert ticks >= 0 : "Precondition: ticks >= 0";

        stopRequested = false;
        var components = clockedComponents();
        try {
            for (final var stop = getTick() + ticks; getTick() < stop && !isStopRequested();) {
                startTick();
                for (var runnable : components) {
                    runnable.run();
                }
            }
        } catch (ThreadFinishedException e) {
            // TODO 2009-12-11 mh: should not happen!!!
        }
    }
}
