package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.clock.CounterComponent;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for {@link SequentialSpinClock}.
 */
public class SequentialSpinClockTest {
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
    clock = new SequentialSpinClock();

    int num = Runtime.getRuntime().availableProcessors() * 3;
    counters = new CounterComponent[num];
    for (int i = 0; i < num; i++) {
      CounterComponent counter = new CounterComponent();
      clock.addClockedComponent(i, counter);
      counters[i] = counter;
    }
  }

  /**
   * Test for {@link SequentialSpinClock#run(int)}.
   */
  @Test
  public void run() {
    clock.run(2);
    for (int i = 0; i < counters.length; i++) {
      CounterComponent counter = counters[i];
      System.out.println(counter.getCount());
    }
  }
}
