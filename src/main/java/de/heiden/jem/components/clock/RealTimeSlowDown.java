package de.heiden.jem.components.clock;

import org.apache.log4j.Logger;

/**
 * Slow down clock to real time.
 */
public class RealTimeSlowDown implements ClockEvent
{
  /**
   * Logger.
   */
  private final Logger _logger = Logger.getLogger(getClass());

  private final Clock _clock;
  private final long _freq;
  private final int _div;
  private final long _incrementFreq;
  private final long _incrementFreq0;
  private final long _incrementTime;
  private final long _incrementTime0;

  /**
   * Timestamp (ns) after last slow down.
   */
  private long _last;
  /**
   * Timestamp (ns) for next slow down.
   */
  private long _next;
  /**
   * Time (ns) elapsed while processing.
   */
  private long _elapsed;
  /**
   * Counter for divs.
   */
  private int _counter;

  /**
   * Constructor.
   *
   * @param clock clock to slow down
   * @param freq frequency in Hz (clock ticks per second)
   * @param div How often per second should the speed be controlled
   */
  public RealTimeSlowDown(Clock clock, long freq, int div)
  {
    assert clock != null : "Precondition: clock != null";
    assert freq > 0 : "Precondition: freq > 0";
    assert div > 0 : "Precondition: div > 0";

    _clock = clock;

    _freq = freq;
    _div = div;
    _incrementFreq = freq / div;
    _incrementFreq0 = freq - (div - 1) * _incrementFreq;
    _incrementTime = 1000000000 / div;
    _incrementTime0 = 1000000000 - (div - 1) * _incrementTime;

    long now = System.nanoTime();
    _last = now;
    _elapsed = 0;
    _counter = 0;
    _next = now;

    _clock.addClockEvent(0, this);
  }

  @Override
  public void execute(long tick)
  {
    // Save the current timestamp as the end of the last processing cycle
    long now = System.nanoTime();
    // Compute duration of the last processing cycle and
    // add it to the duration of processing 1 second
    _elapsed += now - _last;

    long nextTick;
    if (_counter == 0)
    {
      _logger.info("1 simulated second took " + (_elapsed / 1000000) + " ms");
      _elapsed = 0;

      _next += _incrementTime0;
      nextTick = tick + _incrementFreq0;
      _counter = _div - 1;
    }
    else
    {
      _next += _incrementTime;
      nextTick = tick + _incrementFreq;
      _counter--;
    }
    _clock.addClockEvent(nextTick, this);

    if (_logger.isDebugEnabled())
    {
      _logger.debug("tick     : " + tick);
      _logger.debug("next tick: " + nextTick);
      _logger.error("elapsed  : " + (now - _last) + " ns");
      _logger.debug("remainder: " + (_next - now) + " ns");
    }

    // Wait until _next
    long remainder;
    while ((remainder = (_next - now) / 1000000) > 0) // 1 milli second precision
    {
      try
      {
        Thread.sleep(remainder);
        now = System.nanoTime();
      }
      catch (InterruptedException e)
      {
        // ignore
      }
    }

    // Save the current timestamp as the start of next the processing cycle
    _last = now;
  }
}
