package de.heiden.jem.components.clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Base implementation for all clocks.
 */
public abstract class AbstractClock<E extends ClockEntry> implements Clock {
  /**
   * Logger.
   */
  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Has the clock been started?.
   */
  protected boolean _started;

  /**
   * Clocked components.
   */
  protected final SortedMap<Integer, E> _entryMap;

  /**
   * Events.
   */
  private final ClockEvent _events;

  /**
   * Next event to look for.
   * Needs not to be volatile, because only used by clock thread.
   */
  protected long _nextEventTick;

  /**
   * Constructor.
   */
  protected AbstractClock() {
    _started = false;

    _entryMap = new TreeMap<>();

    _events = new RootClockEvent(); // root node, may not be executed
    _events.next = new RootClockEvent(); // end marker, may not be reached
    _nextEventTick = Long.MAX_VALUE;
  }

  @Override
  public void dispose() {
    // overwrite, if needed
  }

  /**
   * Has the clock been started?.
   */
  @Override
  public boolean isStarted() {
    return _started;
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

    logger.debug("add component " + component.getName());
    E entry = createClockEntry(component);
    ClockEntry removed = _entryMap.put(position, entry);
    assert removed == null : "Check: no duplicate positions";

    assert entry.tick != null : "Postcondition: result != null";
    return entry.tick;
  }

  /**
   * Create clock entry.
   */
  protected abstract E createClockEntry(ClockedComponent component);

  /**
   * Add event.
   *
   * @param tick tick to execute event at.
   * @param newEvent event to add.
   * @require tick > getTick()
   * @require newEvent != null
   */
  @Override
  public void addClockEvent(long tick, ClockEvent newEvent) {
    assert tick > getTick() : "tick > getTick()";
    assert newEvent != null : "newEvent != null";
    assert newEvent.next == null : "newEvent.next == null";

//    if (_logger.isDebugEnabled()) {
//      _logger.debug("add event " + newEvent + " at " + tick);
//    }

    newEvent.tick = tick;

    ClockEvent event = _events;
    do {
      ClockEvent next = event.next;
      if (next == null || tick <= next.tick) {
        event.next = newEvent;
        newEvent.next = next;
        updateNextEvent();
        return;
      }

      event = next;
    } while (true);
  }

  /**
   * Remove event.
   *
   * @param oldEvent event to remove
   * @require oldEvent != null
   */
  @Override
  public void removeClockEvent(ClockEvent oldEvent) {
    assert oldEvent != null : "oldEvent != null";

//    if (_logger.isDebugEnabled()) {
//      _logger.debug("remove event " + event);
//    }

    ClockEvent event = _events;
    do {
      ClockEvent next = event.next;
      if (next == oldEvent) {
        event.next = oldEvent.next;
        oldEvent.next = null;
        updateNextEvent();
        return;
      }

      event = next;

    } while (event != null);
  }

  /**
   * Execute current event, if any.
   * <p/>
   * TODO 2010-10-14 mh: remove tick parameter, because it always has to be _nextEventTick...
   *
   * @param tick current clock tick
   */
  protected final void executeEvent(long tick) {
    assert tick == _nextEventTick : "tick == _nextEventTick";

    while (_nextEventTick == tick) {
      // get current event
      ClockEvent event = _events.next;

      // remove it
      ClockEvent nextEvent = event.next;
      _events.next = nextEvent;
      _nextEventTick = nextEvent.tick;
      event.next = null;

      // execute it
//      if (_logger.isDebugEnabled()) {
//        _logger.debug("execute event " + event + " at " + tick);
//      }
      event.execute(tick);
    }
  }

  /**
   * Update _nextEventTick.
   */
  protected final void updateNextEvent() {
    _nextEventTick = _events.next.tick;
  }
}
