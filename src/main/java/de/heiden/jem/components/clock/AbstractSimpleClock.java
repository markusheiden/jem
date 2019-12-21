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

  @Override
  protected long incrementAndGetTick() {
    return ++_tick;
  }

  @Override
  public long getTick() {
    return _tick;
  }
}
