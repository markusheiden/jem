package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.clock.ClockTestBase;

/**
 * Test for {@link ParallelSpinClock}.
 */
public class ParallelSpinClockTest extends ClockTestBase {
  @Override
  protected Clock createClock() {
    return new ParallelSpinClock();
  }
}
