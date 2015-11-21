package de.heiden.jem.components.clock;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base implementation for all clocks.
 */
public abstract class AbstractClock implements Clock {
  /**
   * Logger.
   */
  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Has the clock been started?.
   */
  private final AtomicBoolean _started = new AtomicBoolean(false);

  /**
   * Clocked components.
   */
  protected final SortedMap<Integer, ClockedComponent> _componentMap = new TreeMap<>();

  /**
   * Events.
   */
  private ClockEvent _events;

  /**
   * Next event to look for.
   * Needs not to be volatile, because only used by clock thread.
   */
  long _nextEventTick;

  /**
   * Current tick.
   * Start at tick -1, because the first action when running is to increment the tick.
   */
  protected final AtomicLong _tick = new AtomicLong(-1);

  /**
   * Constructor.
   */
  protected AbstractClock() {
    _events = new RootClockEvent(); // end marker, may not be reached
    _nextEventTick = _events.tick;
  }

  /**
   * Has the clock been started?.
   */
  @Override
  public boolean isStarted() {
    return _started.get();
  }

  /**
   * Add clocked component.
   *
   * @param position position to insert component in execute queue
   * @param component clocked component to add
   * @require position >= 0
   * @require component != null
   * @ensure result != null
   */
  @Override
  public synchronized Tick addClockedComponent(int position, ClockedComponent component) {
    assert component != null : "Precondition: component != null";
    assert position >= 0 : "Precondition: position >= 0";
    assert !isStarted() : "Precondition: !isStarted()";

    logger.debug("add component {}", component.getName());
    Tick tick = createTick(component);
    ClockedComponent removed = _componentMap.put(position, component);
    assert removed == null : "Check: no duplicate positions";

    assert tick != null: "Postcondition: result != null";
    return tick;
  }

  /**
   * Create clock entry.
   */
  protected abstract Tick createTick(ClockedComponent component);

  @Override
  public final void run() {
    logger.debug("run clock");

    init();
    doRun();
  }

  @Override
  public final void run(int ticks) {
    logger.debug("run clock for {} ticks", ticks);

    init();
    doRun(ticks);
  }

  /**
   * Init clock.
   */
  private void init() {
    if (!_started.getAndSet(true)) {
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
   * Run this clock for ever.
   */
  protected abstract void doRun();

  /**
   * Run this clock for a given number of ticks.
   *
   * @param ticks number of ticks to run this clock for
   */
  protected abstract void doRun(int ticks);

  @Override
  public final void close() {
    try {
      doClose();
    } finally {
      _started.set(false);
    }
  }

  /**
   * Dispose clock and all its clocked components.
   */
  protected void doClose() {
    // overwrite, if needed
  }

  @Override
  public void addClockEvent(long tick, ClockEvent newEvent) {
    assert tick > getTick() : "tick > getTick()";
    assert newEvent != null : "newEvent != null";
    assert newEvent.next == null : "newEvent.next == null";

//    if (_logger.isDebugEnabled()) {
//      _logger.debug("add event {} at {}", newEvent, tick);
//    }

    newEvent.tick = tick;

    ClockEvent event = _events;

    if (tick <= event.tick) {
      newEvent.next = event;
      _events = newEvent;
      _nextEventTick = tick;
      return;
    }

    do {
      final ClockEvent next = event.next;
      if (next == null || tick <= next.tick) {
        event.next = newEvent;
        newEvent.next = next;
        // _nextEventTick needs no update
        return;
      }

      event = next;

    } while (true);
  }

  @Override
  public void updateClockEvent(long tick, ClockEvent event) {
    // TODO mh: check, if events needs to be moved. otherwise exit early.
    removeClockEvent(event);
    addClockEvent(tick, event);
  }

  @Override
  public void removeClockEvent(ClockEvent oldEvent) {
    assert oldEvent != null : "oldEvent != null";

//    if (_logger.isDebugEnabled()) {
//      _logger.debug("remove event {}", event);
//    }

    ClockEvent event = _events;

    if (oldEvent == event) {
      final ClockEvent next = event.next;
      _events = next;
      _nextEventTick = next.tick;
      oldEvent.next = null;
      return;
    }

    do {
      final ClockEvent next = event.next;
      if (next == oldEvent) {
        event.next = oldEvent.next;
        // _nextEventTick needs no update
        oldEvent.next = null;
        return;
      }

      event = next;

    } while (event != null);

    // TODO mh: handle case that event is not registered?: oldEvent.next = null;

    assert oldEvent.next == null : "oldEvent.next == null";
  }

  /**
   * Execute current event, if any.
   *
   * @param tick current clock tick
   */
  protected void executeEvent(long tick) {
    while (_nextEventTick == tick) {
      // get current event
      final ClockEvent event = _events;

      // remove it
      final ClockEvent nextEvent = event.next;
      _events = nextEvent;
      _nextEventTick = nextEvent.tick;
      event.next = null;

      // execute it
//      if (_logger.isDebugEnabled()) {
//        _logger.debug("execute event {} at {}", event, tick);
//      }
      event.execute(tick);
    }
  }

  @Override
  public final long getTick() {
    return _tick.get();
  }
}
