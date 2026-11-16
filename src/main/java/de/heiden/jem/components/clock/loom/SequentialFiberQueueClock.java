package de.heiden.jem.components.clock.loom;

import de.heiden.jem.components.clock.AbstractSimpleClock;
import de.heiden.jem.components.clock.ManualAbort;

import java.util.List;
import java.util.concurrent.SynchronousQueue;

/**
 * Clock using {@link Thread fibers} from project loom.
 * The thread running the clock executes the fibers of the components one after another
 * by handing over execution via {@link SynchronousQueue}s.
 */
public final class SequentialFiberQueueClock extends AbstractSimpleClock {
    /**
     * Token handed over to the next thread to execute.
     */
    private static final Object TOKEN = new Object();

    /**
     * Fibers of the components.
     */
    private Thread[] fibers;

    /**
     * Queues of the components followed by the queue of the thread running the clock.
     */
    private SynchronousQueue<Object>[] queues;

    @Override
    @SuppressWarnings("unchecked")
    protected void doInit() {
        var components = clockedComponents();
        var numComponents = components.length;

        queues = new SynchronousQueue[numComponents + 1];
        for (var i = 0; i < queues.length; i++) {
            queues[i] = new SynchronousQueue<>();
        }

        fibers = new Thread[numComponents];
        for (var i = 0; i < numComponents; i++) {
            var component = components[i];
            var queue = queues[i];
            // Execute next component and wait for the next tick. The last component hands over to the clock.
            var next = queues[i + 1];
            component.setTick(() -> executeNext(next, queue));
            fibers[i] = Thread.ofVirtual().name(component.getName()).start(() -> {
                // Wait for the first tick.
                take(queue);
                component.run();
            });
        }
    }

    @Override
    protected void doRun() {
        var first = queues[0];
        var last = queues[queues.length - 1];
        //noinspection InfiniteLoopStatement
        while (true) {
            startTick();
            // Execute first component and wait for the last component to finish the tick.
            executeNext(first, last);
        }
    }

    @Override
    protected void doRun(int ticks) {
        assert ticks >= 0 : "Precondition: ticks >= 0";

        var first = queues[0];
        var last = queues[queues.length - 1];
        for (long stop = getTick() + ticks; getTick() < stop; ) {
            startTick();
            // Execute first component and wait for the last component to finish the tick.
            executeNext(first, last);
        }
    }

    /**
     * Execute next thread and wait for the current thread to execute again.
     */
    private static void executeNext(SynchronousQueue<Object> next, SynchronousQueue<Object> queue) {
        try {
            // Blocks until the next thread takes the token.
            next.put(TOKEN);
        } catch (InterruptedException e) {
            throw new ManualAbort();
        }
        take(queue);
    }

    /**
     * Wait for the token.
     */
    private static void take(SynchronousQueue<Object> queue) {
        try {
            queue.take();
        } catch (InterruptedException e) {
            throw new ManualAbort();
        }
    }

    @Override
    protected void doClose() {
        if (fibers != null) {
            terminate(List.of(fibers));
            fibers = null;
            queues = null;
        }
    }
}
