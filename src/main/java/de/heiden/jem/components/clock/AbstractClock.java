package de.heiden.jem.components.clock;

import org.apache.log4j.Logger;

import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Base implementation for all clocks.
 */
public abstract class AbstractClock<E extends ClockEntry> implements Clock
{
  /**
   * Logger.
   */
  private final Logger _logger = Logger.getLogger(getClass());

  protected boolean _started;

  /**
   * Clocked components.
   * Not synchronized.
   */
  protected final SortedMap<Integer, E> _entryMap;

  /**
   * Events.
   */
  protected final NavigableMap<Long, ClockEvent> _events;

  /**
   * Next event to look for.
   * Needs not to be volatile, because only used by clock thread.
   */
  protected long _nextEventTick;

  /**
   * Constructor.
   */
  protected AbstractClock()
  {
    _started = false;

    _entryMap = new TreeMap<Integer, E>();

    _events = new TreeMap<Long, ClockEvent>();
    _nextEventTick = Long.MAX_VALUE;
    _events.put(_nextEventTick, new ClockEvent()
    {
      @Override
      public void execute(long tick)
      {
        throw new IllegalArgumentException("Dummy event may never be reached");
      }
    });
  }

  @Override
  public void dispose()
  {
    // overwrite, if needed
  }

  public boolean isStarted()
  {
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
  public synchronized Tick addClockedComponent(int position, ClockedComponent component)
  {
    assert component != null : "Precondition: component != null";
    assert position >= 0 : "Precondition: position >= 0";
    assert !isStarted() : "Precondition: !isStarted()";

    _logger.debug("add component " + component.getName());
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
   * @param event event to add.
   * @require tick > getTick()
   * @require listener != null
   */
  public void addClockEvent(long tick, ClockEvent event)
  {
    assert tick > getTick() : "tick > getTick()";
    assert event != null : "event != null";

//    if (_logger.isDebugEnabled())
//    {
//      _logger.debug("add event " + event.toString() + " at " + tick);
//    }

    _events.put(tick, event);
    updateNextEvent();
  }

  /**
   * Remove event.
   *
   * @param event event to remove
   * @require event != null
   */
  public void removeClockEvent(ClockEvent event)
  {
    assert event != null : "event != null";

//    if (_logger.isDebugEnabled())
//    {
//      _logger.debug("remove event " + event.toString());
//    }

    _events.values().remove(event);
    updateNextEvent();
  }

  /**
   * Execute current event, if any.
   */
  protected final void executeEvent(long tick)
  {
    assert tick == _nextEventTick : "tick == _nextEventTick";

    while (_nextEventTick <= tick)
    {
      ClockEvent event = _events.pollFirstEntry().getValue();
//      if (_logger.isDebugEnabled())
//      {
//        _logger.debug("execute event " + event.toString() + " at " + tick);
//      }
      event.execute(tick);
      updateNextEvent();
    }
  }

  /**
   * Update _nextEventTick.
   */
  protected final void updateNextEvent()
  {
    _nextEventTick = _events.firstKey();
  }
}
