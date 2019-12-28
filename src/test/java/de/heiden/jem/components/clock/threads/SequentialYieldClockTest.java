package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.clock.ClockTestBase;

/**
 * Test for {@link SequentialYieldClock}.
 */
class SequentialYieldClockTest extends ClockTestBase {
  @Override
  protected int numCounters() {
    // This clock does not perform well with less threads than components.
    return Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
  }

  @Override
  protected Clock createClock() {
    return new SequentialYieldClock();
  }
}
