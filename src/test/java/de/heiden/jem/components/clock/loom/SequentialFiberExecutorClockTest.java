package de.heiden.jem.components.clock.loom;

import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.clock.ClockTestBase;

/**
 * Test for {@link SequentialFiberExecutorClock}.
 */
class SequentialFiberExecutorClockTest extends ClockTestBase {
    @Override
    protected Clock createClock() {
        return new SequentialFiberExecutorClock();
    }
}
