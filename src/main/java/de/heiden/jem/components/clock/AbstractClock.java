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
  private ClockEvent _events;

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

    _events = new ClockEvent()
    {
      @Override
      public void execute(long tick)
      {
        throw new IllegalArgumentException("Dummy event may never be executed");
      }
    };
    _events.next = null;
    _events.tick = Long.MAX_VALUE;
    _nextEventTick = Long.MAX_VALUE;
  }

  @Override
  public void dispose()
  {
    // overwrite, if needed
  }

  /**
   * Has the clock been started?.
   */
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
   * @param newEvent event to add.
   * @require tick > getTick()
   * @require newEvent != null
   */
  public void addClockEvent(long tick, ClockEvent newEvent)
  {
    assert tick > getTick() : "tick > getTick()";
    assert newEvent != null : "newEvent != null";
    assert newEvent.next == null : "newEvent.next == null";

//    if (_logger.isDebugEnabled())
//    {
//      _logger.debug("add event " + newEvent + " at " + tick);
//    }

    newEvent.tick = tick;

    ClockEvent event = _events;
    if (tick <= _nextEventTick)
    {
      newEvent.next = event;
      _events = newEvent;
    }
    else
    {
      ClockEvent next;
      while ((next = event.next).tick < tick)
      {
        event = next;
      }
      newEvent.next = event.next;
      event.next = newEvent;
    }
    updateNextEvent();
  }

  /**
   * Remove event.
   *
   * @param oldEvent event to remove
   * @require oldEvent != null
   */
  public void removeClockEvent(ClockEvent oldEvent)
  {
    assert oldEvent != null : "oldEvent != null";

//    if (_logger.isDebugEnabled())
//    {
//      _logger.debug("remove event " + event);
//    }

    ClockEvent event = _events;
    if (event == oldEvent)
    {
      _events = oldEvent.next;
    }
    else
    {
      ClockEvent next;
      while ((next = event.next) != oldEvent)
      {
        event = next;
      }
      event.next = oldEvent.next;
    }
    oldEvent.next = null;
    updateNextEvent();
  }

  /**
   * Execute current event, if any.
   *
   * TODO 2010-10-14 mh: removed tick parameter, because it always has to be _nextEventTick...
   * 
   * @param tick current clock tick
   */
  protected final void executeEvent(long tick)
  {
    assert tick == _nextEventTick : "tick == _nextEventTick";

    while (_nextEventTick == tick)
    {
      // get current event
      ClockEvent event = _events;

      // remove it
      ClockEvent nextEvent = event.next;
      _events = nextEvent;
      _nextEventTick = nextEvent.tick;
      event.next = null;

      // execute it
//      if (_logger.isDebugEnabled())
//      {
//        _logger.debug("execute event " + event.toString() + " at " + tick);
//      }
      event.execute(tick);
    }
  }

  /**
   * Update _nextEventTick.
   */
  protected final void updateNextEvent()
  {
    _nextEventTick = _events.tick;
  }
}
