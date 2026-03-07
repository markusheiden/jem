package de.heiden.jem.components.clock.threads;

import de.heiden.jem.components.clock.Clock;
import de.heiden.jem.components.clock.ClockTestBase;
import org.junit.jupiter.api.Disabled;

/**
 * Test for {@link SequentialClock}.
 */
@Disabled("Runs endlessly")
class SequentialClockTest extends ClockTestBase {
    @Override
    protected Clock createClock() {
        return new SequentialClock();
    }
}
