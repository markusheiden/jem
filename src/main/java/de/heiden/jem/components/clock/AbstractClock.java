package de.heiden.jem.components.clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

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
  private final AtomicBoolean _started = new AtomicBoolean(false);

  /**
   * Clocked components.
   */
  protected final SortedMap<Integer, ClockedComponent> _componentMap = new TreeMap<>();

  /**
   * Events.
   */
  final LinkedList<ClockEvent> _events = new LinkedList<>();

  /**
   * Current tick.
   * Start at tick -1, because the first action when running is to increment the tick.
   */
  protected final AtomicLong _tick = new AtomicLong(-1);

  /**
   * Constructor.
   */
  protected AbstractClock() {
    ClockEvent endMarkerEvent = new ClockEvent("End") {
      @Override
      public void execute(long tick) {
        throw new IllegalStateException("End marker event may never be executed.");
      }
    };
    endMarkerEvent.tick = Long.MAX_VALUE;
    _events.add(endMarkerEvent);
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
  public synchronized <C extends ClockedComponent> C addClockedComponent(int position, C component) {
    assert component != null : "Precondition: component != null";
    assert position >= 0 : "Precondition: position >= 0";
    assert !isStarted() : "Precondition: !isStarted()";

    logger.debug("add component {}", component.getName());

    ClockedComponent removed = _componentMap.put(position, component);
    assert removed == null : "Check: no duplicate positions";

    return component;
  }

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

//    if (_logger.isDebugEnabled()) {
//      _logger.debug("add event {} at {}", newEvent, tick);
//    }

    newEvent.tick = tick;

    // Search event position.
    final ListIterator<ClockEvent> iter = _events.listIterator();
    while (tick >= iter.next().tick) {
      // Check next event.
    }

    // Add event directly before its following event.
    iter.previous();
    iter.add(newEvent);
  }

  @Override
  public void updateClockEvent(long tick, ClockEvent eventToUpdate) {
    assert tick > getTick() : "tick > getTick()";
    assert eventToUpdate != null : "eventToUpdate != null";

    if (tick == eventToUpdate.tick) {
      // Nothing to do -> Return early.
      return;
    }

    // TODO mh: check, if events needs to be moved. otherwise exit early.
    _events.remove(eventToUpdate);
    addClockEvent(tick, eventToUpdate);
  }

  @Override
  public void removeClockEvent(ClockEvent oldEvent) {
    assert oldEvent != null : "oldEvent != null";

//    if (_logger.isDebugEnabled()) {
//      _logger.debug("remove event {}", event);
//    }

    _events.remove(oldEvent);
  }

  /**
   * Execute component thread.
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

  /**
   * Start a new tick.
   */
  protected final void startTick() {
    // First increment tick.
    // Second execute events.
    executeEvents(_tick.incrementAndGet());
    // Third execute components.
  }

  /**
   * Execute current events, if any.
   *
   * @param tick current clock tick
   */
  protected void executeEvents(long tick) {
    while (_events.getFirst().tick == tick) {
      // get current event
      final ClockEvent event = _events.removeFirst();

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
