package de.heiden.jem.components.clock;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

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
    assertSame(event2, clock.getNextEvent());
    assertEquals(2, clock.getNextEventTick());

    // add event before first event -> next tick should be set to tick of this event
    clock.addClockEvent(1, event1);
    assertSame(event1, clock.getNextEvent());
    assertEquals(1, clock.getNextEventTick());

    // add event after first event -> next tick should not change
    clock.addClockEvent(3, event3);
    assertSame(event1, clock.getNextEvent());
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
    assertSame(event20, event10.next);
    assertSame(event10, clock.getNextEvent());
    assertEquals(10, clock.getNextEventTick());
    // event10 is still first, but the next event tick needs to be updated.
    clock.updateClockEvent(11, event10);
    assertSame(event20, event10.next);
    assertSame(event10, clock.getNextEvent());
    assertEquals(11, clock.getNextEventTick());
    // event10 is no longer the first event -> the next event (event20) takes its place.
    clock.updateClockEvent(21, event10);
    assertSame(event10, event20.next);
    assertSame(event30, event10.next);
    assertSame(event20, clock.getNextEvent());
    assertEquals(20, clock.getNextEventTick());
    // event10 is now the last event.
    clock.updateClockEvent(31, event10);
    assertSame(event10, event30.next);
    assertSame(event20, clock.getNextEvent());
    assertEquals(20, clock.getNextEventTick());
  }

  @Test
  public void removeClockEvent_first() {
    TestClock clock = new TestClock();
    ClockEvent event = new TestClockEvent();

    // Remove not registered event -> nothing should happen
    clock.removeClockEvent(event);
    assertEquals(Long.MAX_VALUE, clock.getNextEventTick());
    assertNull(event.next);

    clock.addClockEvent(1, event);
    assertSame(event, clock.getNextEvent());
    assertEquals(1, clock.getNextEventTick());
    assertNotNull(event.next);

    // Remove first registered event -> next tick should be reset
    clock.removeClockEvent(event);
    assertEquals(Long.MAX_VALUE, clock.getNextEventTick());
    assertNull(event.next);
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
    assertSame(event1, clock.getNextEvent());
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
//    assertNull(event2a.next);
    assertTrue(executed.contains(event2b));
//    assertNull(event2b.next);
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
  }

  /**
   * Clock event implementation for testing.
   */
  private class TestClockEvent extends ClockEvent {
    /**
     * Constructor.
     */
    public TestClockEvent() {
      super("Test event");
    }

    @Override
    public void execute(long tick) {
      // Record executions
      executed.add(this);
    }
  }
}
