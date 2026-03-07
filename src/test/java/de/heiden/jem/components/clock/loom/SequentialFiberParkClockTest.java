package de.heiden.jem.components.clock.loom;

import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.clock.ClockTestBase;

/**
 * Test for {@link SequentialFiberParkClock}.
 */
class SequentialFiberParkClockTest extends ClockTestBase {
    @Override
    protected Clock createClock() {
        return new SequentialFiberParkClock();
    }
}
