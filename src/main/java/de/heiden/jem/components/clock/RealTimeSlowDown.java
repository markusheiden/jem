package de.heiden.jem.components.clock;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Slow down the clock to real time.
 */
public class RealTimeSlowDown extends ClockEvent {
    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Clock to slow down.
     */
    private final Clock clock;
    /**
     * Frequency (ticks per second) of clock.
     */
    private final long freq;
    /**
     * How often per second should the clock speed be adjusted? (minus 1).
     */
    private final int div;
    /**
     * Number of ticks per div.
     */
    private final long incrementFreq;
    /**
     * Number of ticks for the last div to avoid rounding errors.
     */
    private final long incrementFreq0;
    /**
     * Nanoseconds per div.
     */
    private final long incrementTime;
    /**
     * Nanoseconds for the last div to avoid rounding errors.
     */
    private final long incrementTime0;

    /**
     * Timestamp (ns) after the last slow down.
     */
    private long lastTimestamp;
    /**
     * Timestamp (ns) for the next slow down.
     */
    private long nextTimestamp;
    /**
     * Time (ns) elapsed while processing 1 second (accumulator).
     */
    private long elapsed;
    /**
     * Counter for divs.
     */
    private int counter;

    /**
     * Constructor.
     *
     * @param clock
     *         Clock to slow down
     * @param freq
     *         Frequency in Hz (clock ticks per second)
     * @param div
     *         How often per second should the clock speed be adjusted?
     */
    public RealTimeSlowDown(@Nonnull Clock clock, long freq, int div) {
        super("Real time slow down");

        assert freq > 0 : "Precondition: freq > 0";
        assert div > 0 : "Precondition: div > 0";
        assert freq / div >= 1000 : "Precondition: freq / div >= 1000: Maximum timer resolution of 1 ms not exceeded.";

        this.clock = clock;

        this.freq = freq;
        this.div = div - 1;
        incrementFreq = freq / div;
        incrementFreq0 = freq - (div - 1) * incrementFreq;
        incrementTime = 1000000000 / div;
        incrementTime0 = 1000000000 - (div - 1) * incrementTime;

        // Initial clock event
        this.clock.addClockEvent(0, new ClockEvent("Init real time slow down") {
            @Override
            public void execute(long tick) {
                long now = System.nanoTime();
                elapsed = 0;

                counter = RealTimeSlowDown.this.div;
                nextTimestamp = now + incrementTime0;
                RealTimeSlowDown.this.clock.addClockEvent(incrementFreq0, RealTimeSlowDown.this);

                lastTimestamp = now;
            }
        });
    }

    @Override
    public void execute(long tick) {
        // Save the current timestamp as the end of the last processing cycle
        long now = System.nanoTime();
        // Compute the duration of the last processing cycle and add it to the duration of processing 1 second
        elapsed += now - lastTimestamp;

        long next = nextTimestamp;
        long nextTick = tick;
        if (counter == 0) {
            if (logger.isInfoEnabled()) {
                logger.info("1 simulated second took {} ms", elapsed / 1000000);
            }
            elapsed = 0;

            counter = div;
            next += incrementTime0;
            nextTick += incrementFreq0;
        } else {
            counter--;
            next += incrementTime;
            nextTick += incrementFreq;
        }
        // If emulation is too slow, do not accumulate missing time.
        nextTimestamp = next < now ? now : next;
        // Re-register for the next tick.
        clock.addClockEvent(nextTick, this);

        // Debug the slow-down.
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("tick      : %,11d", tick));
            logger.debug(String.format("next tick : %,11d", nextTick));
            logger.debug(String.format("elapsed   : %,11d ns", now - lastTimestamp));
            logger.debug(String.format("remainder : %,11d ns", nextTimestamp - now));
            logger.debug(String.format("real time : %,11d ns", incrementTime));
        }

        // Wait until next (max. 1 ms precision).
        for (long remainder; (remainder = (next - now) / 1000000) > 0; now = System.nanoTime()) {
            try {
                Thread.sleep(remainder);
            } catch (InterruptedException e) {
                // Stop slowing down if the thread has been interrupted.
                break;
            }
        }

        // Save the current timestamp as the start of the next the processing cycle.
        lastTimestamp = now;
    }
}
