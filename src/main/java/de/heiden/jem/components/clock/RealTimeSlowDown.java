package de.heiden.jem.components.clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Slow down clock to real time.
 */
public class RealTimeSlowDown extends ClockEvent {
  /**
   * Logger.
   */
  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Clock to slow down.
   */
  private final Clock _clock;
  /**
   * Frequency (ticks per second) of clock.
   */
  private final long _freq;
  /**
   * How often per second should the clock speed be adjusted? (minus 1).
   */
  private final int _div;
  /**
   * Number of ticks per div.
   */
  private final long _incrementFreq;
  /**
   * Number of ticks for the last div to avoid rounding errors.
   */
  private final long _incrementFreq0;
  /**
   * Nanoseconds per div.
   */
  private final long _incrementTime;
  /**
   * Nanoseconds for the last div to avoid rounding errors.
   */
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
   * Time (ns) elapsed while processing 1 second (accumulator).
   */
  private long _elapsed;
  /**
   * Counter for divs.
   */
  private int _counter;

  /**
   * Constructor.
   *
   * @param clock Clock to slow down
   * @param freq Frequency in Hz (clock ticks per second)
   * @param div How often per second should the clock speed be adjusted?
   */
  public RealTimeSlowDown(Clock clock, long freq, int div) {
    super("Real time slow down");

    assert clock != null : "Precondition: clock != null";
    assert freq > 0 : "Precondition: freq > 0";
    assert div > 0 : "Precondition: div > 0";
    assert freq / div >= 1000 : "Precondition: freq / div >= 1000: Maximum timer resolution of 1 ms not exceeded.";

    _clock = clock;

    _freq = freq;
    _div = div - 1;
    _incrementFreq = freq / div;
    _incrementFreq0 = freq - (div - 1) * _incrementFreq;
    _incrementTime = 1000000000 / div;
    _incrementTime0 = 1000000000 - (div - 1) * _incrementTime;

    // Initial clock event
    _clock.addClockEvent(0, new ClockEvent("Init real time slow down") {
      @Override
      public void execute(long tick) {
        long now = System.nanoTime();
        _elapsed = 0;

        _counter = _div;
        _next = now + _incrementTime0;
        _clock.addClockEvent(_incrementFreq0, RealTimeSlowDown.this);

        _last = now;
      }
    });
  }

  @Override
  public void execute(long tick) {
    // Save the current timestamp as the end of the last processing cycle
    long now = System.nanoTime();
    // Compute duration of the last processing cycle and add it to the duration of processing 1 second
    _elapsed += now - _last;

    long next = _next;
    long nextTick = tick;
    if (_counter == 0) {
      if (logger.isInfoEnabled()) {
        logger.info("1 simulated second took {} ms",_elapsed / 1000000);
      }
      _elapsed = 0;

      _counter = _div;
      next += _incrementTime0;
      nextTick += _incrementFreq0;
    } else {
      _counter--;
      next += _incrementTime;
      nextTick += _incrementFreq;
    }
    // if emulation is too slow, do not accumulate missing time
    _next = next < now ? now : next;
    // re-register for next tick
    _clock.addClockEvent(nextTick, this);

    // debug slow down
    if (logger.isDebugEnabled()) {
      logger.debug(String.format("tick      : %,11d", tick));
      logger.debug(String.format("next tick : %,11d", nextTick));
      logger.debug(String.format("elapsed   : %,11d ns", now - _last));
      logger.debug(String.format("remainder : %,11d ns", _next - now));
      logger.debug(String.format("real time : %,11d ns", _incrementTime));
    }

    // Wait until next (max. 1 ms precision)
    for (long remainder; (remainder = (next - now) / 1000000) > 0; now = System.nanoTime())
    {
      try {
        Thread.sleep(remainder);
      } catch (InterruptedException e) {
        // stop slowing down, if thread has been interrupted
        break;
      }
    }

    // Save the current timestamp as the start of next the processing cycle
    _last = now;
  }
}
