package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.Tick;

import java.util.concurrent.locks.LockSupport;

import static java.util.concurrent.locks.LockSupport.unpark;

/**
 * Clock implemented with synchronization sequentially executing component threads.
 */
public final class SequentialClock extends AbstractSynchronizedClock {
    /**
     * Ordinal of component thread to execute.
     * Package visible to avoid synthetic accessors.
     */
    volatile int state = 0;

    @Override
    protected void doSynchronizedInit() {
        var components = clockedComponents();
        Thread firstThread = null;
        SequentialTick previousTick = null;
        for (int i = 0; i < components.length; i++) {
            final int state = i;
            final int nextState = i + 1;

            var component = components[state];
            var tick = new SequentialTick(state);
            component.setTick(tick);

            // Start component.
            var thread = createStartedDaemonThread(component.getName(), () -> executeComponent(component, tick));
            if (firstThread == null) {
                firstThread = thread;
            }
            if (previousTick != null) {
                previousTick.nextThread = thread;
            }
            // Wait for component to reach first tick.
            waitForState(nextState);

            previousTick = tick;
        }

        // Start tick manager.
        var finalFirstThread = firstThread;
        var tickThread = createDaemonThread("Tick", () -> executeTicks(components.length, finalFirstThread));
        previousTick.nextThread = tickThread;
        tickThread.start();
    }

    /**
     * Execution of ticks.
     */
    private void executeTicks(final int state, final Thread nextThread) {
        //noinspection InfiniteLoopStatement
        while (true) {
            startTick();
            // Execute all component threads.
            this.state = 0;
            unpark(nextThread);
            // Wait for all component threads to finish tick.
            waitForState(state);
        }
    }

    /**
     * Busy wait until the state is reached.
     * Package visible to avoid synthetic accessors.
     *
     * @param state
     *         State to reach.
     */
    void waitForState(final int state) {
        do {
            // There is no problem, if this thread is not parked when the previous threads unparks it:
            // In this case this park will not block, see LockSupport.unpark() javadoc.
            LockSupport.park(this);
        } while (this.state != state);
    }

    /**
     * Special tick, waiting for its state and parking its thread but unparking the next thread before.
     */
    private final class SequentialTick implements Tick {
        /**
         * Ordinal of component thread.
         */
        private final int tickState;

        /**
         * Thread of the next component.
         * First this is the current thread to avoid parking the thread executing {@link #doInit()}.
         */
        private Thread nextThread = Thread.currentThread();

        /**
         * Constructor.
         *
         * @param state
         *         Ordinal of thread to execute.
         */
        private SequentialTick(int state) {
            this.tickState = state;
        }

        @Override
        public void waitForTick() {
            final int tickState = this.tickState;
            // Execute next component thread.
            state = tickState + 1;
            unpark(nextThread);
            // Wait for the next tick.
            waitForState(tickState);
        }
    }
}
