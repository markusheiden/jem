package de.heiden.jem.components.clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base implementation for all clocks that do not need synchronization.
 */
public abstract class AbstractSimpleClock extends AbstractClock {
  /**
   * Logger.
   */
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Current tick.
   * Start at tick -1, because the first action when running is to increment the tick.
   */
  private long tick = -1;

  /**
   * Start a new tick.
   */
  protected final void startTick() {
    // First: Increment tick.
    // Second: Execute events.
    executeEvents(++tick);
    // Third: Execute components: Done by the caller.
  }

  @Override
  public long getTick() {
    return tick;
  }
}
