package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.Tick;
import de.heiden.jem.components.clock.loom.FiberScheduler;

import java.util.concurrent.locks.LockSupport;

import static de.heiden.jem.components.clock.ManualAbort.abortIfInterrupted;
import static java.lang.Thread.onSpinWait;
import static java.util.concurrent.locks.LockSupport.unpark;

/**
 * Clock implemented with synchronization sequentially executing component threads.
 * The thread of the last component starts the next tick, so there is no separate tick manager thread.
 * <p>
 * All components and events are executed by the same thread, if the component threads of this JVM are limited to a
 * {@link FiberScheduler#useSingleCarrier() single carrier thread}. Otherwise, a warning is logged.
 */
public final class SequentialClock extends AbstractSynchronizedClock {
    /**
     * Ordinal of component thread to execute.
     * Package visible to avoid synthetic accessors.
     */
    volatile int state = 0;

    /**
     * Thread of the first component.
     * Set before the thread of the last component, which is the only one reading it, is started.
     */
    private Thread firstThread;

    @Override
    protected boolean isSequential() {
        return true;
    }

    @Override
    protected void doSynchronizedInit() {
        var components = clockedComponents();
        var last = components.length - 1;
        Thread firstThread = null;
        SequentialTick previousTick = null;
        for (var i = 0; i < last; i++) {
            var state = i;
            var nextState = i + 1;

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

        // Start the last component. It starts the first tick when reaching its first tick.
        var component = components[last];
        Tick tick = () -> {
            startTick();
            // Execute first component thread.
            state = 0;
            unpark(this.firstThread);
            // Wait for the next tick.
            waitForState(last);
        };
        component.setTick(tick);
        var lastThread = createDaemonThread(component.getName(), () -> executeComponent(component, tick));
        this.firstThread = firstThread != null ? firstThread : lastThread;
        if (previousTick != null) {
            previousTick.nextThread = lastThread;
        }
        lastThread.start();
    }

    /**
     * Wait until the state is reached.
     * First busy waits, because parking and unparking a thread is expensive.
     * Package visible to avoid synthetic accessors.
     *
     * @param state
     *         State to reach.
     */
    void waitForState(int state) {
        for (var i = spinLimit(); i > 0; i--) {
            if (this.state == state) {
                return;
            }
            onSpinWait();
        }
        while (this.state != state) {
            // There is no problem, if this thread is not parked when the previous threads unparks it:
            // In this case this park will not block, see LockSupport.unpark() javadoc.
            LockSupport.park(this);
            // park() does not block anymore, if the thread has been interrupted.
            abortIfInterrupted();
        }
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
            var tickState = this.tickState;
            // Execute next component thread.
            state = tickState + 1;
            unpark(nextThread);
            // Wait for the next tick.
            waitForState(tickState);
        }
    }
}
