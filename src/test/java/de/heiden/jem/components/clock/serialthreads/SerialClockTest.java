package de.heiden.jem.components.clock.serialthreads;

import de.heiden.jem.components.clock.ClockTestBase;
import org.junit.runner.RunWith;
import org.serialthreads.agent.Transform;
import org.serialthreads.agent.TransformingRunner;
import org.serialthreads.transformer.strategies.frequent3.FrequentInterruptsTransformer3;

/**
 * Test for {@link SerialClock}.
 */
@RunWith(TransformingRunner.class)
@Transform(transformer = FrequentInterruptsTransformer3.class, classPrefixes = "de.heiden.jem")
public class SerialClockTest extends ClockTestBase {
  @Override
  protected SerialClock createClock() throws Exception {
    return new SerialClock();
  }
}
