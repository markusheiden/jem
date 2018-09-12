package de.heiden.jem.components.clock;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test base for {@link Clock}s.
 */
public abstract class ClockTestBase {
  /**
   * Create clock.
   */
  protected abstract Clock createClock() throws Exception;

  /**
   * Test for {@link Clock#run(int)}.
   */
  @Test
  void run() throws Exception {
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
    boolean failure = false;
    for (int i = 0; i < counters.length; i++) {
      failure |= counters[i].getCount() != cycles;
    }

    if (failure) {
      for (int i = 0; i < counters.length; i++) {
        System.out.println(String.format("Counter %d: %d", i, counters[i].getCount()));
      }
      fail("Not all counters are at " + cycles);
    }
  }
}
