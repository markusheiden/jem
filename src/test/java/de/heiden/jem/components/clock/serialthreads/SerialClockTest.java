package de.heiden.jem.components.clock.serialthreads;

import de.heiden.jem.components.clock.ClockTestBase;
import org.serialthreads.agent.Transform;

/**
 * Test for {@link SerialClock}.
 */
@Transform(classPrefixes = "de.heiden.jem")
class SerialClockTest extends ClockTestBase {
    @Override
    protected SerialClock createClock() {
        return new SerialClock();
    }
}
