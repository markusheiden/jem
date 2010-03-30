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
  private final long _incrementTime;
  private final long _incrementFreq1;

  private long _last;
  private long _elapsed;
  private int _counter;
  private long _next;

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
    _incrementFreq1 = freq - (div - 1) * _incrementFreq;
    _incrementTime = 1000000000 / div;

    long now = System.nanoTime();
    _last = now;
    _elapsed = 0;
    _counter = div;
    _next = now + _incrementTime;

    _clock.addClockEvent(0 + _incrementFreq1, this);
  }

  @Override
  public void execute(long tick)
  {
    long now = System.nanoTime();
    _elapsed += (now - _last) / 1000000;

    long incrementTick;
    if (_counter == 1)
    {
      _logger.info("1 simulated second took " + _elapsed + " ms");
      _elapsed = 0;
      _counter = _div;
      incrementTick = _incrementFreq1;
    }
    else
    {
      _counter--;
      incrementTick = _incrementFreq;
    }
    _clock.addClockEvent(tick + incrementTick, this);

    if (_logger.isDebugEnabled())
    {
      _logger.debug("tick     : " + tick);
      _logger.debug("next tick: " + (tick + incrementTick));
      _logger.error("elapsed  : " + ((now - _last) / 1000000) + " ms");
      _logger.debug("remainder: " + ((_next - now) / 1000000) + " ms");
    }

    long remainder;
    while ((remainder = (_next - now) / 1000000) > 1) // 1 milli second precision
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

    _last = now;
    _next += _incrementTime;
  }
}
