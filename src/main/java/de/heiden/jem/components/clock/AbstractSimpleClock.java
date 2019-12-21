package de.heiden.jem.components.clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base implementation for all clocks.
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
  private long _tick = -1;

  /**
   * Start a new tick.
   */
  protected final void startTick() {
    // First increment tick.
    // Second execute events.
    executeEvents(++_tick);
    // Third execute components.
  }

  @Override
  public long getTick() {
    return _tick;
  }
}
