package de.heiden.jem.components.clock;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base implementation for all clocks.
 */
public abstract class AbstractClock implements Clock {
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
     *         position to insert component in execute queue
     * @param component
     *         clocked component to add
     * @require position >= 0
     * @require component != null
     * @ensure result != null
     */
    @Override
    public synchronized <C extends ClockedComponent> C addClockedComponent(int position, @Nonnull C component) {
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
        // overwrite, if needed
    }

    /**
     * Run this clock forever.
     */
    protected abstract void doRun();

    /**
     * Run this clock for a given number of ticks.
     *
     * @param ticks
     *         number of ticks to run this clock for
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
        // overwrite, if needed
    }

    @Override
    public void addClockEvent(final long tick, final @Nonnull ClockEvent newEvent) {
        assert tick > getTick() : "tick > getTick()";

//    if (_logger.isDebugEnabled()) {
//      _logger.debug("Add event {} at {}.", newEvent, tick);
//    }

        newEvent.tick = tick;

        var nextEvent = events;
        while (tick > nextEvent.tick) {
            nextEvent = nextEvent.next;
            // search further
        }

        if (nextEvent == events) {
            events = newEvent;
        } else {
            var previousEvent = nextEvent.previous;
            previousEvent.next = newEvent;
            newEvent.previous = previousEvent;
        }
        newEvent.next = nextEvent;
        nextEvent.previous = newEvent;
    }

    @Override
    public void updateClockEvent(final long tick, final @Nonnull ClockEvent eventToUpdate) {
        assert tick > getTick() : "tick > getTick()";

        if (tick == eventToUpdate.tick) {
            // Nothing to do -> Return early.
            return;
        }

        // TODO mh: check, if events needs to be moved. otherwise exit early.
        removeClockEvent(eventToUpdate);
        addClockEvent(tick, eventToUpdate);
    }

    @Override
    public void removeClockEvent(final @Nonnull ClockEvent oldEvent) {
//    if (_logger.isDebugEnabled()) {
//      _logger.debug("Remove event {}.", event);
//    }

        var previousEvent = oldEvent.previous;
        var nextEvent = oldEvent.next;

        if (oldEvent == events) {
            events = nextEvent;
        } else if (previousEvent != null) {
            previousEvent.next = nextEvent;
        }
        if (nextEvent != null) {
            nextEvent.previous = previousEvent;
        }
    }

    /**
     * Next event that gets executed.
     */
    final ClockEvent getNextEvent() {
        return events;
    }

    /**
     * Execute current events, if any.
     *
     * @param tick
     *         current clock tick
     */
    protected void executeEvents(final long tick) {
        for (var event = events; event.tick == tick; event = events) {
            // Remove it.
            events = event.next;

            // Execute it.
//      if (_logger.isDebugEnabled()) {
//        _logger.debug("Execute event {} at {}.", event, tick);
//      }
            event.execute(tick);
        }
    }
}
