package de.heiden.jem.components.clock;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test base for {@link Clock}s.
 */
public abstract class ClockTestBase {
  /**
   * Create clock.
   */
  protected abstract Clock createClock();

  /**
   * Test for {@link Clock#run(int)}.
   */
  @Test
  public void run() {
    int runs = 10;
    int cycles = 1000;
    for (int i = 0; i < runs; i++) {
      try (Clock clock = createClock()) {
        run(clock, cycles);
      }
    }
  }

  /**
   * Test run clock.
   */
  private void run(Clock clock, int cycles) {
    int num = Runtime.getRuntime().availableProcessors() * 3;
    CounterComponent[] counters = new CounterComponent[num];
    for (int i = 0; i < num; i++) {
      CounterComponent counter = new CounterComponent();
      clock.addClockedComponent(i, counter);
      counters[i] = counter;
    }

    clock.run(cycles);

    // Check that al components are executed exactly the specified amount of cycles.
    for (int i = 0; i < counters.length; i++) {
      assertEquals(cycles, counters[i].getCount());
    }
  }
}
