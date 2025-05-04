package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.clock.ClockTestBase;

import static java.lang.Math.max;

/**
 * Test for {@link SequentialSpinClock}.
 */
class SequentialSpinClockTest extends ClockTestBase {
    @Override
    protected int numCounters() {
        // This clock does not perform well with fewer threads than components.
        return max(1, Runtime.getRuntime().availableProcessors() / 2);
    }

    @Override
    protected Clock createClock() {
        return new SequentialSpinClock();
    }
}
