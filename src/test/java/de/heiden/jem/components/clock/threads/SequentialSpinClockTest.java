package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.clock.ClockTestBase;

/**
 * Test for {@link SequentialSpinClock}.
 */
public class SequentialSpinClockTest extends ClockTestBase {
  @Override
  protected Clock createClock() {
    return new SequentialSpinClock();
  }
}