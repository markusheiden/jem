package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.clock.ClockTestBase;
import org.junit.jupiter.api.Disabled;

/**
 * Test for {@link ParallelNotifyClock}.
 */
@Disabled("Runs endlessly")
class ParallelNotifyClockTest extends ClockTestBase {
    @Override
    protected Clock createClock() {
        return new ParallelNotifyClock();
    }
}
