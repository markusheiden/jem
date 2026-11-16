package de.heiden.jem.components.clock.loom;

import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.clock.ClockTestBase;
import org.junit.jupiter.api.Disabled;

/**
 * Test for {@link SequentialFiberExecutorClock}.
 */
@Disabled("Thread.run() does not step a started virtual thread and there is no public API for custom schedulers.")
class SequentialFiberExecutorClockTest extends ClockTestBase {
    @Override
    protected Clock createClock() {
        return new SequentialFiberExecutorClock();
    }
}
