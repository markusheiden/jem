package de.heiden.jem.components.clock.serialthreads;

import de.heiden.jem.components.clock.ClockTestBase;
import org.junit.runner.RunWith;
import org.serialthreads.agent.TransformingTestRunner;
import org.serialthreads.transformer.IStrategy;
import org.serialthreads.transformer.Strategies;

/**
 * Test for {@link SerialClock}.
 */
@RunWith(TransformingTestRunner.class)
public class SerialClockTest extends ClockTestBase {
  /**
   * Transforming strategy.
   */
  public static IStrategy getStrategy() {
    return Strategies.DEFAULT;
  }

  /**
   * Prefixes of classes to transform.
   */
  public static String[] getClassPrefixes() {
    return new String[]{ "de.heiden.jem" };
  }

  @Override
  protected SerialClock createClock() throws Exception {
    return new SerialClock();
  }
}
