package de.heiden.jem.components.clock;

import org.apache.log4j.Logger;

/**
 * Stop watch for execution performance.
 * Measures time each simulated second.
 */
public class StopWatch implements ClockEvent
{
  /**
   * Logger.
   */
  private final Logger _logger = Logger.getLogger(getClass());

  private final Clock _clock;
  private final long _freq;

  private long _last;

  /**
   * Constructor.
   *
   * @param clock clock to slow down
   * @param freq frequency in Hz (clock ticks per second)
   */
  public StopWatch(Clock clock, long freq)
  {
    assert clock != null : "Precondition: clock != null";
    assert freq > 0 : "Precondition: freq > 0";

    _clock = clock;
    _freq = freq;
    long now = System.nanoTime();
    _last = now;

    _clock.addClockEvent(_freq, this);
  }

  @Override
  public void execute(long tick)
  {
    long now = System.nanoTime();
    long elapsed = (now - _last) / 1000000;
    _logger.info("1 simulated second took " + elapsed + " ms");
    _last = now;

    _clock.addClockEvent(tick + _freq, this);
  }
}
