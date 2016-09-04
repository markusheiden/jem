package de.heiden.jem.components.clock;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for {@link AbstractClock}
 */
public class AbstractClockTest {
  /**
   * List of executed events.
   */
  private final List<ClockEvent> executed = new ArrayList<>();

  @Test
  public void constructor() {
    TestClock clock = new TestClock();
    assertEquals(Long.MAX_VALUE, clock.getNextEventTick());
  }

  @Test
  public void addClockEvent() {
    TestClock clock = new TestClock();
    ClockEvent event1 = new TestClockEvent();
    ClockEvent event2 = new TestClockEvent();
    ClockEvent event3 = new TestClockEvent();

    // add first event -> next tick should be set to tick of this event
    clock.addClockEvent(2, event2);
    assertEquals(2, clock.getNextEventTick());

    // add event before first event -> next tick should be set to tick of this event
    clock.addClockEvent(1, event1);
    assertEquals(1, clock.getNextEventTick());

    // add event after first event -> next tick should not change
    clock.addClockEvent(3, event3);
    assertEquals(1, clock.getNextEventTick());
  }

  @Test
  public void updateClockEvent() {
    TestClock clock = new TestClock();
    ClockEvent event10 = new TestClockEvent();
    ClockEvent event20 = new TestClockEvent();
    ClockEvent event30 = new TestClockEvent();

    clock.addClockEvent(10, event10);
    clock.addClockEvent(20, event20);
    clock.addClockEvent(30, event30);

    // Nothing to update.
    clock.updateClockEvent(10, event10);
    assertEquals(10, clock.getNextEventTick());
    // event10 is still first, but the next event tick needs to be updated.
    clock.updateClockEvent(11, event10);
    assertEquals(11, clock.getNextEventTick());
    // event10 is no longer the first event -> the next event (event20) takes its place.
    clock.updateClockEvent(21, event10);
    assertEquals(20, clock.getNextEventTick());
    // event10 is now the last event.
    clock.updateClockEvent(31, event10);
    assertEquals(20, clock.getNextEventTick());
  }

  @Test
  public void removeClockEvent_first() {
    TestClock clock = new TestClock();
    ClockEvent event = new TestClockEvent();

    // Remove not registered event -> nothing should happen
    clock.removeClockEvent(event);
    assertEquals(Long.MAX_VALUE, clock.getNextEventTick());

    clock.addClockEvent(1, event);
    assertEquals(1, clock.getNextEventTick());

    // Remove first registered event -> next tick should be reset
    clock.removeClockEvent(event);
    assertEquals(Long.MAX_VALUE, clock.getNextEventTick());
  }

  @Test
  public void removeClockEvent_many() {
    TestClock clock = new TestClock();
    ClockEvent event1 = new TestClockEvent();
    ClockEvent event2 = new TestClockEvent();

    clock.addClockEvent(1, event1);
    clock.addClockEvent(2, event2);
    assertEquals(1, clock.getNextEventTick());

    // Remove second registered event -> next tick should still be the tick of the first event
    clock.removeClockEvent(event2);
    assertEquals(1, clock.getNextEventTick());
  }

  @Test
  public void execute() {
    TestClock clock = new TestClock();
    ClockEvent event2a = new TestClockEvent();
    clock.addClockEvent(2, event2a);
    ClockEvent event2b = new TestClockEvent();
    clock.addClockEvent(2, event2b);
    ClockEvent event3 = new TestClockEvent();
    clock.addClockEvent(3, event3);
    assertEquals(0, executed.size());

    // Execute tick 1, which has no registered events -> nothing should happen
    clock.executeEvents(1);
    assertEquals(0, executed.size());

    // Execute tick 2, which has two registered events -> both should be executed
    clock.executeEvents(2);
    assertEquals(2, executed.size());
    assertTrue(executed.contains(event2a));
    assertTrue(executed.contains(event2b));
  }

  /**
   * Clock implementation for testing.
   */
  private static class TestClock extends AbstractClock {
    @Override
    protected void doRun() {
      throw new UnsupportedOperationException("Not implemented for test clock");
    }

    @Override
    protected void doRun(int ticks) {
      throw new UnsupportedOperationException("Not implemented for test clock");
    }

    /**
     * Tick, when the next event gets executed.
     */
    long getNextEventTick() {
      return _events.first().tick;
    }
  }

  /**
   * Clock event implementation for testing.
   */
  private class TestClockEvent extends ClockEvent {
    /**
     * Constructor.
     */
    TestClockEvent() {
      super("Test event");
    }

    @Override
    public void execute(long tick) {
      // Record executions
      executed.add(this);
    }
  }
}
