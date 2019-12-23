package de.heiden.jem.components.clock;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for {@link AbstractClock}
 */
public class AbstractClockTest {
  /**
   * List of executed events.
   */
  private final List<ClockEvent> executed = new ArrayList<>();

  @Test
  void constructor() {
    TestClock clock = new TestClock();
    assertEquals(Long.MAX_VALUE, clock.getNextEventTick());
  }

  @Test
  void addClockEvent() {
    TestClock clock = new TestClock();
    ClockEvent event1 = new TestClockEvent();
    ClockEvent event2 = new TestClockEvent();
    ClockEvent event3 = new TestClockEvent();

    // Add first event -> Next tick should be set to tick of this event.
    clock.addClockEvent(2, event2);
    assertNull(event2.previous);
    assertNotNull(event2.next);
    assertSame(event2, clock.getNextEvent());
    assertEquals(2, clock.getNextEventTick());

    // Add event before first event -> Next tick should be set to tick of this event.
    clock.addClockEvent(1, event1);
    assertNull(event1.previous);
    assertSame(event2, event1.next);
    assertSame(event1, event2.previous);
    assertSame(event1, clock.getNextEvent());
    assertEquals(1, clock.getNextEventTick());

    // Add event after first event -> Next tick should not change.
    clock.addClockEvent(3, event3);
    assertSame(event3, event2.next);
    assertSame(event2, event3.previous);
    assertNotNull(event3.next);
    assertSame(event1, clock.getNextEvent());
    assertEquals(1, clock.getNextEventTick());
  }

  @Test
  void updateClockEvent() {
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
  void removeClockEvent_notAdded() {
    TestClock clock = new TestClock();
    ClockEvent event1 = new TestClockEvent();
    ClockEvent event2 = new TestClockEvent();

    clock.addClockEvent(2, event2);

    // Remove not registered event -> No change.
    clock.removeClockEvent(event1);
    assertNull(event1.previous);
    assertNull(event1.next);
    assertSame(event2, clock.getNextEvent());
    assertEquals(2, clock.getNextEventTick());
  }

  @Test
  void removeClockEvent_only() {
    TestClock clock = new TestClock();
    ClockEvent event1 = new TestClockEvent();

    clock.addClockEvent(1, event1);

    // Remove only registered event -> Next tick should be reset.
    clock.removeClockEvent(event1);
    assertNull(event1.previous);
    assertEquals(Long.MAX_VALUE, clock.getNextEventTick());
  }

  @Test
  void removeClockEvent_first() {
    TestClock clock = new TestClock();
    ClockEvent event1 = new TestClockEvent();
    ClockEvent event2 = new TestClockEvent();

    clock.addClockEvent(1, event1);
    clock.addClockEvent(2, event2);

    // Remove first registered event -> Next tick should be reset.
    clock.removeClockEvent(event1);
    assertNull(event1.previous);
    assertNull(event2.previous);
    assertNotNull(event2.next);
    assertSame(event2, clock.getNextEvent());
    assertEquals(2, clock.getNextEventTick());
  }

  @Test
  void removeClockEvent_many() {
    TestClock clock = new TestClock();
    ClockEvent event1 = new TestClockEvent();
    ClockEvent event2 = new TestClockEvent();
    ClockEvent event3 = new TestClockEvent();

    clock.addClockEvent(1, event1);
    clock.addClockEvent(2, event2);
    clock.addClockEvent(3, event3);
    assertEquals(1, clock.getNextEventTick());

    // Remove second registered event -> next tick should still be the tick of the first event
    clock.removeClockEvent(event2);
    assertSame(event3, event1.next);
    assertSame(event1, event3.previous);
    assertSame(event1, clock.getNextEvent());
    assertEquals(1, clock.getNextEventTick());
  }

  @Test
  void execute() {
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
  private static class TestClock extends AbstractSimpleClock {
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
    final long getNextEventTick() {
      return getNextEvent().tick;
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
