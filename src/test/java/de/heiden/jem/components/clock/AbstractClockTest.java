package de.heiden.jem.components.clock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

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
  public void removeClockEvent_first() {
    TestClock clock = new TestClock();
    ClockEvent event = new TestClockEvent();

    // Remove not registered event -> nothing should happen
    clock.removeClockEvent(event);
    assertEquals(Long.MAX_VALUE, clock.getNextEventTick());
    assertNull(event.next);

    clock.addClockEvent(1, event);
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
    clock.executeEvent(1);
    assertEquals(0, executed.size());

    // Execute tick 2, which has two registered events -> both should be executed
    clock.executeEvent(2);
    assertEquals(2, executed.size());
    assertTrue(executed.contains(event2a));
    assertNull(event2a.next);
    assertTrue(executed.contains(event2b));
    assertNull(event2b.next);
  }

  /**
   * Clock implementation for testing.
   */
  private static class TestClock extends AbstractClock<ClockEntry> {
    /**
     * Current tick.
     */
    private long tick = 0;

    @Override
    protected ClockEntry createClockEntry(ClockedComponent component) {
      return new ClockEntry(component, () -> {
        // do nothing
      });
    }

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
    public long getNextEventTick() {
      return _nextEventTick;
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
