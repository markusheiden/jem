package de.heiden.jem.components.clock.loom;

import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.clock.ClockTestBase;

import static java.lang.Math.max;
import static java.lang.Runtime.getRuntime;

/**
 * Test for {@link SequentialFiberYieldClock}.
 */
class SequentialFiberYieldClockTest extends ClockTestBase {
    @Override
    protected int numCounters() {
        // This clock does not perform well with fewer threads than components.
        return max(1, getRuntime().availableProcessors() / 2);
    }

    @Override
    protected Clock createClock() {
        return new SequentialFiberYieldClock();
    }
}
