package de.heiden.jem.components.clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Thread.State.NEW;
import static java.lang.Thread.currentThread;
import static java.util.Objects.requireNonNull;

/**
 * Base implementation for all clocks.
 */
public abstract class AbstractClock implements Clock {
    /**
     * Maximum time to wait for the termination of all threads of a clock.
     */
    private static final Duration TERMINATION_TIMEOUT = Duration.ofSeconds(5);

    /**
     * Logger.
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Has the clock been started?.
     */
    private final AtomicBoolean started = new AtomicBoolean(false);

    /**
     * Clocked components.
     */
    private final SortedMap<Integer, ClockedComponent> componentMap = new TreeMap<>();

    /**
     * Events.
     */
    private ClockEvent events;

    /**
     * Constructor.
     */
    protected AbstractClock() {
        events = new ClockEvent("End") {
            @Override
            public void execute(long tick) {
                throw new IllegalStateException("End marker event may never be executed.");
            }
        };
        events.tick = Long.MAX_VALUE;
    }

    /**
     * Has the clock been started?.
     */
    @Override
    public boolean isStarted() {
        return started.get();
    }

    /**
     * Add a clocked component.
     *
     * @param position
     *         Position to insert component in execute queue.
     * @param component
     *         Clocked component to add.
     * @require position >= 0
     */
    @Override
    public synchronized <C extends ClockedComponent> C addClockedComponent(int position, C component) {
        assert position >= 0 : "Precondition: position >= 0";
        assert !isStarted() : "Precondition: !isStarted()";

        logger.debug("add component {}", component.getName());

        var removed = componentMap.put(position, component);
        assert removed == null : "Check: no duplicate positions";

        return component;
    }

    /**
     * All clocked components in order.
     */
    protected ClockedComponent[] clockedComponents() {
        return componentMap.values().toArray(ClockedComponent[]::new);
    }

    @Override
    public final void run() {
        logger.debug("run clock");

        init();
        try {
            doRun();
        } catch (ManualAbort a) {
            // Do not throw if manually terminated.
        }
    }

    @Override
    public final void run(int ticks) {
        logger.debug("run clock for {} ticks", ticks);

        init();
        try {
            doRun(ticks);
        } catch (ManualAbort a) {
            // Do not throw if manually terminated.
        }
    }

    /**
     * Init clock.
     */
    private void init() {
        if (!started.getAndSet(true)) {
            doInit();
        }
    }

    /**
     * Init clock.
     */
    protected void doInit() {
        // Overwrite, if needed.
    }

    /**
     * Run this clock forever.
     */
    protected abstract void doRun();

    /**
     * Run this clock for a given number of ticks.
     *
     * @param ticks
     *         Number of ticks to run this clock for.
     */
    protected abstract void doRun(int ticks);

    /**
     * Execute the component thread.
     */
/*
  @Interruptible
  protected final void executeComponent(ClockedComponent component, Tick tick) {
    logger.debug("starting {}", component.getName());
    tick.waitForTick();
    logger.debug("started {}", component.getName());
    component.run();
  }
*/
    @Override
    public final void close() {
        try {
            doClose();
        } finally {
            started.set(false);
        }
    }

    /**
     * Dispose the clock and all its clocked components.
     */
    protected void doClose() {
        // Overwrite, if needed.
    }

    /**
     * Run the task of a thread of this clock until it gets manually aborted.
     * A manual abort is the regular way to terminate the threads of a clock, so it is not reported.
     */
    protected static void runUntilAbort(Runnable task) {
        try {
            task.run();
        } catch (ManualAbort e) {
            // Let the thread terminate.
        }
    }

    /**
     * Interrupt the threads and wait for their termination.
     * The threads are expected to terminate, when they are interrupted while waiting for a tick.
     *
     * @param threads
     *         Threads to terminate.
     * @throws IllegalStateException
     *         If a thread did not terminate in time.
     */
    protected static void terminate(Iterable<Thread> threads) {
        threads.forEach(Thread::interrupt);
        var deadline = System.nanoTime() + TERMINATION_TIMEOUT.toNanos();
        threads.forEach(thread -> terminate(thread, deadline));
    }

    private static void terminate(Thread thread, long deadline) {
        try {
            if (thread.getState() != NEW &&
                !thread.join(Duration.ofNanos(deadline - System.nanoTime()))) {
                throw new IllegalStateException("Thread " + thread.getName() + " did not terminate.");
            }
        } catch (InterruptedException e) {
            currentThread().interrupt();
            throw new ManualAbort();
        }
    }

    @Override
    public void addClockEvent(long tick, ClockEvent newEvent) {
        assert tick > getTick() : "tick > getTick()";

//    if (logger.isDebugEnabled()) {
//      logger.debug("Add event {} at {}.", newEvent, tick);
//    }

        if (newEvent.next != null) {
            // The event is already registered -> Move it. Otherwise, the list of events would get corrupted.
            removeClockEvent(newEvent);
        }
        newEvent.tick = tick;

        var nextEvent = events;
        while (tick > nextEvent.tick) {
            // The end marker has the maximum tick, so all events before it have a next event.
            nextEvent = requireNonNull(nextEvent.next);
            // Search further.
        }

        if (nextEvent == events) {
            events = newEvent;
        } else {
            // Not the first event, so it has a previous event.
            var previousEvent = requireNonNull(nextEvent.previous);
            previousEvent.next = newEvent;
            newEvent.previous = previousEvent;
        }
        newEvent.next = nextEvent;
        nextEvent.previous = newEvent;
    }

    @Override
    public void updateClockEvent(long tick, ClockEvent eventToUpdate) {
        assert tick > getTick() : "tick > getTick()";

        if (tick == eventToUpdate.tick) {
            // Nothing to do -> Return early.
            return;
        }

        // TODO mh: Check, if events needs to be moved. Otherwise exit early.
        // Adding a registered event moves it.
        addClockEvent(tick, eventToUpdate);
    }

    @Override
    public void removeClockEvent(ClockEvent oldEvent) {
//    if (logger.isDebugEnabled()) {
//      logger.debug("Remove event {}.", event);
//    }

        var previousEvent = oldEvent.previous;
        var nextEvent = oldEvent.next;
        if (nextEvent == null) {
            // All registered events have a next event, at least the end marker.
            // The event is not registered, e.g. because it has already been executed or removed -> Nothing to do.
            return;
        }

        if (previousEvent == null) {
            events = nextEvent;
        } else {
            previousEvent.next = nextEvent;
        }
        nextEvent.previous = previousEvent;

        // Unlink the event to mark it as not registered.
        oldEvent.previous = null;
        oldEvent.next = null;
    }

    /**
     * Next event that gets executed.
     */
    final ClockEvent getNextEvent() {
        return events;
    }

    /**
     * Tick of the next event that gets executed.
     */
    protected final long nextEventTick() {
        return events.tick;
    }

    /**
     * Execute current events, if any.
     *
     * @param tick
     *         Current clock tick.
     */
    protected void executeEvents(long tick) {
        for (var event = events; event.tick == tick; event = events) {
            // Remove it and unlink it to mark it as not registered.
            // This has to happen before executing it, because events may re-register themselves.
            // The end marker is never executed, so all executed events have a next event.
            var nextEvent = requireNonNull(event.next);
            events = nextEvent;
            nextEvent.previous = null;
            event.next = null;

            // Execute it.
//      if (logger.isDebugEnabled()) {
//        logger.debug("Execute event {} at {}.", event, tick);
//      }
            event.execute(tick);
        }
    }
}
