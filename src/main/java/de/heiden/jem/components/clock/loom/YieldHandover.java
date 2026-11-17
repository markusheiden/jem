package de.heiden.jem.components.clock.loom;

import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.LockSupport;

import static de.heiden.jem.components.clock.ManualAbort.abortIfInterrupted;

/**
 * Hands over execution between threads, which wait for their execution by yielding.
 * Threads waiting for long park instead of yielding:
 * Yielding threads keep their carrier thread busy, so threads woken up from blocking or sleeping,
 * e.g. when slowing down to real time, would never get executed again, if the other threads yielded forever.
 */
public final class YieldHandover {
    /**
     * Number of yields to wait for execution before parking.
     */
    private static final int YIELD_LIMIT = 1_000;

    /**
     * Number of threads that execute one after another in a ring. These are the threads with the lowest indices.
     */
    private final int ring;

    /**
     * Index of the thread that is allowed to execute.
     */
    private volatile int current;

    /**
     * Threads that are parked or are about to park by their index.
     */
    private final AtomicReferenceArray<Thread> parked;

    /**
     * Constructor.
     *
     * @param numThreads
     *         Number of threads
     * @param ring
     *         Number of threads that execute one after another in a ring
     * @param first
     *         Index of the thread that executes first
     */
    public YieldHandover(int numThreads, int ring, int first) {
        this.ring = ring;
        this.current = first;
        this.parked = new AtomicReferenceArray<>(numThreads);
    }

    /**
     * Is the thread allowed to execute?
     *
     * @param index
     *         Index of the thread
     */
    public boolean isCurrent(int index) {
        return current == index;
    }

    /**
     * Let the thread execute.
     *
     * @param index
     *         Index of the thread to execute
     */
    public void resume(int index) {
        current = index;
        // Parking threads first register themselves and then check the current thread,
        // so here either the registration is visible or they see that they are allowed to execute.
        if (parked.get(index) != null) {
            unpark(index);
            if (index < ring) {
                // Most probably the other threads of the ring are parked too.
                // Unpark them in the order of their execution: On a single carrier thread yielding threads execute
                // in the order they were scheduled, so each thread yields just once until it is allowed to execute.
                // Unparking each thread not until it is allowed to execute results in the reverse order.
                for (var i = 1; i < ring; i++) {
                    unpark((index + i) % ring);
                }
            }
        }
    }

    private void unpark(int index) {
        var thread = parked.getAndSet(index, null);
        if (thread != null) {
            LockSupport.unpark(thread);
        }
    }

    /**
     * Wait until the current thread is allowed to execute.
     *
     * @param index
     *         Index of the current thread
     */
    public void await(int index) {
        while (true) {
            for (var i = YIELD_LIMIT; i > 0; i--) {
                if (current == index) {
                    return;
                }
                Thread.yield();
                abortIfInterrupted();
            }

            parked.set(index, Thread.currentThread());
            if (current != index) {
                // There is no problem, if this thread is not parked when it gets unparked:
                // In this case this park will not block, see LockSupport.unpark() javadoc.
                LockSupport.park(this);
            }
            parked.set(index, null);
            // park() does not block anymore, if the thread has been interrupted.
            abortIfInterrupted();
        }
    }
}
