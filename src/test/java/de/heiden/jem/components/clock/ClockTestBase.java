package de.heiden.jem.components.clock;

import de.heiden.jem.components.clock.threads.SequentialSpinClock;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test for {@link SequentialSpinClock}.
 */
public abstract class ClockTestBase {
  /**
   * Clock under test.
   */
  private Clock clock;

  /**
   * Counter components.
   */
  private CounterComponent[] counters;

  /**
   * Set up.
   */
  @Before
  public void setUp() {
    clock = createClock();

    int num = Runtime.getRuntime().availableProcessors() * 3;
    counters = new CounterComponent[num];
    for (int i = 0; i < num; i++) {
      CounterComponent counter = new CounterComponent();
      clock.addClockedComponent(i, counter);
      counters[i] = counter;
    }
  }

  protected abstract Clock createClock();

  /**
   * Test for {@link SequentialSpinClock#run(int)}.
   */
  @Test
  public void run() {
    int cycles = 10_000;
    clock.run(cycles);
    for (int i = 0; i < counters.length; i++) {
      assertEquals(cycles, counters[i].getCount());
    }
  }
}
