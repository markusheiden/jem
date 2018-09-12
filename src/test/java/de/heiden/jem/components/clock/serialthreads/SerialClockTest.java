package de.heiden.jem.components.clock.serialthreads;

import de.heiden.jem.components.clock.ClockTestBase;
import org.serialthreads.agent.Transform;
import org.serialthreads.transformer.strategies.frequent3.FrequentInterruptsTransformer3;

/**
 * Test for {@link SerialClock}.
 */
@Transform(transformer = FrequentInterruptsTransformer3.class, classPrefixes = "de.heiden.jem")
class SerialClockTest extends ClockTestBase {
  @Override
  protected SerialClock createClock() {
    return new SerialClock();
  }
}
